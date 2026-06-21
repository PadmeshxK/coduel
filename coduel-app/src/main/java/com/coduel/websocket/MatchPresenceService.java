package com.coduel.websocket;

import com.coduel.api.MatchApi;
import com.coduel.api.MatchParticipantApi;
import com.coduel.api.UserApi;
import com.coduel.entity.MatchParticipant;
import com.coduel.flow.MatchFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.MatchEventPublisher;
import com.coduel.model.constant.MatchEndReason;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Forfeit-on-disconnect for duels. The WebSocket connection is the presence signal — the server
 * detects a drop even on browser/tab close. A grace window absorbs refreshes (disconnect -> quick
 * reconnect); if the player is still gone after it, they forfeit and the opponent wins.
 */
@Service
@Log4j2
public class MatchPresenceService {

    private static final String MATCH_TOPIC_PREFIX = "/topic/match/";
    // Grace so a refresh/transient drop doesn't count as leaving.
    private static final long FORFEIT_GRACE_MS = 30_000;
    // After a match is created, how long a player has to actually show up (subscribe) before a no-show.
    private static final long START_GRACE_MS = 15_000;

    @Autowired
    private UserApi userApi;
    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchFlow matchFlow;
    @Autowired
    private MatchParticipantApi matchParticipantApi;
    @Autowired
    private MatchEventPublisher matchEventPublisher;

    // sessionId -> who is watching which match
    private final Map<String, Sub> sessions = new ConcurrentHashMap<>();
    // matches that have had both players present at least once (the duel actually started)
    private final Set<Long> readyMatches = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "match-forfeit");
        thread.setDaemon(true);
        return thread;
    });

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(MATCH_TOPIC_PREFIX)) {
            return;
        }
        // Read the principal from the message headers (same source the subscription interceptor uses) —
        // event.getUser() can be null here even when the session is authenticated.
        Principal principal = accessor.getUser();
        if (principal == null) {
            log.warn("Presence subscribe has no principal for {}", destination);
            return;
        }
        try {
            Long userId = userApi.getCheckByGoogleId(principal.getName()).getId();
            Long matchId = Long.parseLong(destination.substring(MATCH_TOPIC_PREFIX.length()));
            sessions.put(accessor.getSessionId(), new Sub(userId, matchId));
            log.info("WS presence: user {} present in match {}", userId, matchId);
            publishReadyIfAllPresent(matchId);
        } catch (Exception e) {
            log.warn("Presence subscribe failed for {}: {}", destination, e.getMessage());
        }
    }

    // When the subscribe that just happened means every participant is now present, tell both
    // clients the duel can begin (also re-fires on a reconnect, which is harmless on the client).
    // Only fires for ACTIVE DUEL matches — private rooms are started explicitly by the host.
    private void publishReadyIfAllPresent(Long matchId) {
        com.coduel.entity.Match match;
        try {
            match = matchApi.getCheckById(matchId);
        } catch (Exception e) {
            return;
        }
        if (match.getState() != com.coduel.model.constant.MatchState.ACTIVE
                || match.getGameMode() != com.coduel.model.constant.GameMode.DUEL) {
            return;
        }
        Set<Long> present = presentUserIds(matchId);
        List<MatchParticipant> participants = matchParticipantApi.getByMatchId(matchId);
        boolean allPresent = !participants.isEmpty()
                && participants.stream().allMatch(p -> present.contains(p.getUserId()));
        log.info("Match {} readiness check: present={}, allPresent={}", matchId, present, allPresent);
        if (allPresent) {
            // Only flag readiness inside the start window. The flag exists solely so the no-show
            // deadline can tell "never showed" from "showed then left"; once the deadline passes it's
            // useless, so we don't re-add it on later reconnects — that keeps readyMatches bounded to
            // matches in their first few seconds instead of growing forever as matches accumulate.
            if (match.getCreatedAt() != null
                    && Duration.between(match.getCreatedAt(), Instant.now()).toMillis() < START_GRACE_MS) {
                readyMatches.add(matchId);
            }
            // Delay slightly: the subscription that triggered this may not be fully registered with
            // the broker yet, so an immediate send could miss the player who just subscribed.
            scheduler.schedule(
                    () -> matchEventPublisher.publish(matchId, ConversionHelper.toMatchReadyEvent()),
                    300, TimeUnit.MILLISECONDS);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Sub sub = sessions.remove(event.getSessionId());
        if (sub == null) {
            return;
        }
        // Forfeit only if they haven't reconnected by the time this fires (i.e. not a refresh).
        scheduler.schedule(
                () -> forfeitIfAbsent(sub.userId, sub.matchId), FORFEIT_GRACE_MS, TimeUnit.MILLISECONDS);
    }

    private void forfeitIfAbsent(Long userId, Long matchId) {
        if (isPresent(userId, matchId)) {
            return; // reconnected within the grace window
        }
        try {
            // Only an in-progress match can be decided by a disconnect (a LOBBY room hasn't started).
            if (matchApi.getCheckById(matchId).getState() != com.coduel.model.constant.MatchState.ACTIVE) {
                return;
            }
            // Mode-agnostic, N-player rule: a disconnect just means that player is no longer present.
            // The match ends only when presence collapses to one (that player wins) or zero (void).
            // For a 1v1 duel this is exactly "the opponent wins"; for an N-player room the game plays
            // on until a single player remains — or until someone solves it first (finish() is
            // idempotent, so a solve that already ended the match makes this a no-op).
            Set<Long> present = presentUserIds(matchId);
            List<Long> remaining = matchParticipantApi.getByMatchId(matchId).stream()
                    .map(MatchParticipant::getUserId)
                    .filter(present::contains)
                    .toList();

            if (remaining.size() == 1) {
                Long winner = remaining.get(0);
                if (matchFlow.finish(matchId, winner, MatchEndReason.OPPONENT_FORFEIT)) {
                    markLosersForfeited(matchId, winner);
                    matchEventPublisher.publish(matchId,
                            ConversionHelper.toMatchOverEvent(winner, MatchEndReason.OPPONENT_FORFEIT));
                    log.info("Match {}: user {} wins (everyone else disconnected)", matchId, winner);
                }
            } else if (remaining.isEmpty() && matchApi.expire(matchId, MatchEndReason.NO_SHOW_VOID)) {
                matchEventPublisher.publish(matchId,
                        ConversionHelper.toMatchOverEvent(null, MatchEndReason.NO_SHOW_VOID));
                log.info("Match {} voided (all players disconnected)", matchId);
            }
            // remaining.size() >= 2 -> the match plays on.
        } catch (Exception e) {
            log.warn("Forfeit check failed for match {}: {}", matchId, e.getMessage());
        }
    }

    // Called when a match is created: if a player never shows up (subscribes) within the start grace,
    // they no-show and the player who did show wins by walkover (or it voids if nobody showed).
    public void scheduleStartDeadline(Long matchId) {
        scheduler.schedule(() -> enforceStartDeadline(matchId), START_GRACE_MS, TimeUnit.MILLISECONDS);
    }

    private void enforceStartDeadline(Long matchId) {
        try {
            // Only DUEL matches have an enforced start deadline; private rooms use explicit host start.
            com.coduel.entity.Match matchCheck = matchApi.getCheckById(matchId);
            if (matchCheck.getGameMode() != com.coduel.model.constant.GameMode.DUEL) return;
            // If everyone already showed up at least once, this isn't a no-show situation — the match
            // started properly. Anyone leaving afterwards is a disconnect-forfeit (30s grace), NOT a
            // no-show. Without this guard, a player showing then leaving within 15s wrongly walkovers.
            // remove() also frees the entry — this deadline is its only reader, so it's done with now.
            if (readyMatches.remove(matchId)) return;
            Set<Long> present = presentUserIds(matchId);
            List<Long> presentParticipants = matchParticipantApi.getByMatchId(matchId).stream()
                    .map(MatchParticipant::getUserId)
                    .filter(present::contains)
                    .toList();

            if (presentParticipants.size() >= 2) {
                return; // everyone showed up — let it play out
            }
            if (presentParticipants.size() == 1) {
                Long winner = presentParticipants.get(0);
                if (matchFlow.finish(matchId, winner, MatchEndReason.OPPONENT_NO_SHOW)) {
                    markLosersForfeited(matchId, winner);
                    matchEventPublisher.publish(matchId,
                            ConversionHelper.toMatchOverEvent(winner, MatchEndReason.OPPONENT_NO_SHOW));
                    log.info("Match {} won by walkover (opponent no-show) -> user {}", matchId, winner);
                }
            } else if (matchApi.expire(matchId, MatchEndReason.NO_SHOW_VOID)) {
                // nobody showed up -> void the match
                matchEventPublisher.publish(matchId,
                        ConversionHelper.toMatchOverEvent(null, MatchEndReason.NO_SHOW_VOID));
                log.info("Match {} voided (no players showed up)", matchId);
            }
        } catch (Exception e) {
            log.warn("Start-deadline check failed for match {}: {}", matchId, e.getMessage());
        }
    }

    // Mark every non-winner as forfeited (DB flag + live PLAYER_FORFEIT) so the winner's scoreboard
    // shows who dropped out. A terminal duel forfeit/no-show otherwise fires only MATCH_OVER, leaving
    // the loser's row unmarked. Idempotent — players already flagged are just re-broadcast.
    private void markLosersForfeited(Long matchId, Long winnerId) {
        for (MatchParticipant participant : matchParticipantApi.getByMatchId(matchId)) {
            if (participant.getUserId().equals(winnerId)) {
                continue;
            }
            if (!participant.isForfeit()) {
                try {
                    matchParticipantApi.forfeitUserByMatchIdAndUserId(matchId, participant.getUserId());
                } catch (Exception ignored) {
                    // already flagged / race — the live event below still updates scoreboards
                }
            }
            matchEventPublisher.publish(matchId,
                    ConversionHelper.toPlayerForfeitEvent(participant.getUserId()));
        }
    }

    private Set<Long> presentUserIds(Long matchId) {
        return sessions.values().stream()
                .filter(s -> s.matchId.equals(matchId))
                .map(s -> s.userId)
                .collect(Collectors.toSet());
    }

    /**
     * True if the user is currently watching a match that belongs to the given room. Lobby presence
     * uses this so a player in the room's arena — actively playing OR lingering on the match-over
     * screen before auto-returning — still counts as "in the room" and isn't kicked just because
     * they've left the lobby topic. Without it, a kick scheduled at match start fires during the
     * post-match linger (no active match yet) and removes everyone, closing the room.
     */
    public boolean isUserInRoomMatch(Long userId, Long roomId) {
        Set<Long> watchedMatchIds = sessions.values().stream()
                .filter(s -> s.userId.equals(userId))
                .map(s -> s.matchId)
                .collect(Collectors.toSet());
        for (Long matchId : watchedMatchIds) {
            try {
                if (roomId.equals(matchApi.getCheckById(matchId).getRoomId())) {
                    return true;
                }
            } catch (Exception ignored) {
                // match no longer exists — treat as not present in this room's arena
            }
        }
        return false;
    }

    private boolean isPresent(Long userId, Long matchId) {
        return sessions.values().stream()
                .anyMatch(s -> s.userId.equals(userId) && s.matchId.equals(matchId));
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private static final class Sub {
        final Long userId;
        final Long matchId;

        Sub(Long userId, Long matchId) {
            this.userId = userId;
            this.matchId = matchId;
        }
    }
}
