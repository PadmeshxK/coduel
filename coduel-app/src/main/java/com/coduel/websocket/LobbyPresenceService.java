package com.coduel.websocket;

import com.coduel.api.MatchApi;
import com.coduel.api.UserApi;
import com.coduel.flow.RoomFlow;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.RoomEventPublisher;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lobby presence: a member is "in" a room while subscribed to /topic/room/{id}. If they disconnect
 * and don't return within the grace window, they're removed from the room (and can't rejoin — the
 * subscription interceptor and getView both require membership). A disconnect while a match is in
 * progress is just the lobby->arena handoff, so we re-check after the match instead of kicking.
 */
@Service
@Log4j2
public class LobbyPresenceService {

    private static final String ROOM_TOPIC_PREFIX = "/topic/room/";
    // Grace so a refresh / transient drop doesn't kick someone out of the lobby.
    private static final long KICK_GRACE_MS = 30_000;

    @Autowired
    private UserApi userApi;
    @Autowired
    private MatchApi matchApi;
    @Autowired
    private MatchPresenceService matchPresenceService;
    @Autowired
    private RoomFlow roomFlow;
    @Autowired
    private RoomEventPublisher roomEventPublisher;

    // (sessionId + ":" + subscriptionId) -> which room that subscription watches. Keyed per
    // SUBSCRIPTION, not per session: the whole app shares ONE WebSocket, so a member "leaves" a room
    // by UNSUBSCRIBING from its topic (the socket stays open) — a plain disconnect no longer means
    // "left the room", it only fires on tab-close/logout.
    private final Map<String, Sub> subs = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "lobby-presence");
        thread.setDaemon(true);
        return thread;
    });

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX)) {
            return;
        }
        // Only the bare room topic is the lobby-presence signal — ignore sub-topics like .../chat
        // (presence is tracked on /topic/room/{id}; a sub-topic id parse would otherwise just fail).
        if (destination.indexOf('/', ROOM_TOPIC_PREFIX.length()) >= 0) {
            return;
        }
        Principal principal = accessor.getUser();
        if (principal == null) {
            return;
        }
        try {
            Long userId = userApi.getCheckByGoogleId(principal.getName()).getId();
            Long roomId = Long.parseLong(destination.substring(ROOM_TOPIC_PREFIX.length()));
            subs.put(subKey(accessor.getSessionId(), accessor.getSubscriptionId()), new Sub(userId, roomId));
        } catch (Exception e) {
            log.warn("Lobby subscribe failed for {}: {}", destination, e.getMessage());
        }
    }

    // Left the room's topic (navigated away) — the socket stays open on the shared connection, so this
    // UNSUBSCRIBE is how we learn someone left the lobby. Without it, an empty room is never swept.
    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Sub sub = subs.remove(subKey(accessor.getSessionId(), accessor.getSubscriptionId()));
        if (sub != null) {
            scheduler.schedule(() -> removeIfAbsent(sub.userId, sub.roomId), KICK_GRACE_MS, TimeUnit.MILLISECONDS);
        }
    }

    // Socket fully closed (tab close / logout) — drop every room subscription it held and re-evaluate.
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String prefix = event.getSessionId() + ":";
        List<Sub> gone = new ArrayList<>();
        subs.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                gone.add(entry.getValue());
                return true;
            }
            return false;
        });
        for (Sub sub : gone) {
            scheduler.schedule(() -> removeIfAbsent(sub.userId, sub.roomId), KICK_GRACE_MS, TimeUnit.MILLISECONDS);
        }
    }

    private String subKey(String sessionId, String subscriptionId) {
        return sessionId + ":" + subscriptionId;
    }

    private void removeIfAbsent(Long userId, Long roomId) {
        if (isPresent(userId, roomId)) {
            return; // reconnected / refreshed within grace (back in the lobby topic)
        }
        try {
            // They left the lobby topic to play, not to quit — keep watching and re-evaluate later
            // instead of kicking, while either:
            //  - they're in this room's arena (playing, or lingering on the match-over screen before
            //    auto-returning) — covers the post-match window where there's no active match yet, or
            //  - a match is still in progress in this room (covers a player who fully dropped mid-match
            //    but should stay a member until that match resolves).
            if (matchPresenceService.isUserInRoomMatch(userId, roomId)
                    || matchApi.findActiveByRoomId(roomId) != null) {
                scheduler.schedule(() -> removeIfAbsent(userId, roomId), KICK_GRACE_MS, TimeUnit.MILLISECONDS);
                return;
            }
            boolean closed = roomFlow.removeMember(roomId, userId);
            roomEventPublisher.publish(roomId,
                    closed ? ConversionHelper.toRoomClosed() : ConversionHelper.toRoomRosterChanged());
            log.info("Lobby presence: removed absent user {} from room {} (closed={})", userId, roomId, closed);
        } catch (Exception e) {
            log.warn("Lobby kick failed for room {}: {}", roomId, e.getMessage());
        }
    }

    private boolean isPresent(Long userId, Long roomId) {
        return subs.values().stream()
                .anyMatch(s -> s.userId.equals(userId) && s.roomId.equals(roomId));
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private static final class Sub {
        final Long userId;
        final Long roomId;

        Sub(Long userId, Long roomId) {
            this.userId = userId;
            this.roomId = roomId;
        }
    }
}
