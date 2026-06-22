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

import java.security.Principal;
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

    // sessionId -> who is in which room
    private final Map<String, Sub> sessions = new ConcurrentHashMap<>();
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
            sessions.put(accessor.getSessionId(), new Sub(userId, roomId));
        } catch (Exception e) {
            log.warn("Lobby subscribe failed for {}: {}", destination, e.getMessage());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Sub sub = sessions.remove(event.getSessionId());
        if (sub == null) {
            return;
        }
        scheduler.schedule(() -> removeIfAbsent(sub.userId, sub.roomId), KICK_GRACE_MS, TimeUnit.MILLISECONDS);
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
        return sessions.values().stream()
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
