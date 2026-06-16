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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private void publishReadyIfAllPresent(Long matchId) {
        Set<Long> present = presentUserIds(matchId);
        List<MatchParticipant> participants = matchParticipantApi.getByMatchId(matchId);
        boolean allPresent = !participants.isEmpty()
                && participants.stream().allMatch(p -> present.contains(p.getUserId()));
        log.info("Match {} readiness check: present={}, allPresent={}", matchId, present, allPresent);
        if (allPresent) {
            readyMatches.add(matchId);
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
            Long opponent = opponentOf(matchId, userId);
            // finish() is idempotent: no-op if the match already ended (e.g. someone solved it).
            if (Objects.nonNull(opponent) && matchFlow.finish(matchId, opponent, MatchEndReason.OPPONENT_FORFEIT)) {
                matchEventPublisher.publish(matchId,
                        ConversionHelper.toMatchOverEvent(opponent, MatchEndReason.OPPONENT_FORFEIT));
                log.info("Match {} forfeited by user {} -> user {} wins", matchId, userId, opponent);
            }
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

    private Set<Long> presentUserIds(Long matchId) {
        return sessions.values().stream()
                .filter(s -> s.matchId.equals(matchId))
                .map(s -> s.userId)
                .collect(Collectors.toSet());
    }

    private boolean isPresent(Long userId, Long matchId) {
        return sessions.values().stream()
                .anyMatch(s -> s.userId.equals(userId) && s.matchId.equals(matchId));
    }

    private Long opponentOf(Long matchId, Long userId) {
        for (MatchParticipant participant : matchParticipantApi.getByMatchId(matchId)) {
            if (!participant.getUserId().equals(userId)) {
                return participant.getUserId();
            }
        }
        return null;
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
