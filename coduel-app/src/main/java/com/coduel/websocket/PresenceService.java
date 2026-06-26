package com.coduel.websocket;

import com.coduel.api.FriendshipApi;
import com.coduel.api.UserApi;
import com.coduel.entity.Friendship;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.PresencePublisher;
import com.coduel.model.data.PresenceData;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Global online/offline presence. The shared WebSocket is the signal: a user is "online" while they
 * hold at least one live session (multiple tabs = multiple sessions). Online↔offline transitions are
 * broadcast to that user's accepted friends over /user/queue/presence, so their chat and friends
 * views light up live. A short grace on disconnect absorbs refreshes so presence doesn't flicker.
 *
 * In-memory + single instance (same as MatchPresenceService / LobbyPresenceService) — the WS sessions
 * all live on this node, so there's nothing to coordinate across the cluster yet.
 */
@Service
@Log4j2
public class PresenceService {

    // Absorbs a refresh / transient drop before declaring someone offline.
    private static final long OFFLINE_GRACE_MS = 8_000;

    @Autowired
    private UserApi userApi;
    @Autowired
    private FriendshipApi friendshipApi;
    @Autowired
    private PresencePublisher presencePublisher;

    // sessionId -> userId for every live WS session (a user with N tabs has N entries).
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "presence");
        thread.setDaemon(true);
        return thread;
    });

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        register(accessor.getUser(), accessor.getSessionId());
    }

    // Fallback: in some setups the principal isn't populated on the CONNECT event (so onConnect would
    // never broadcast → presence looked "static until reload"). The principal IS reliably present on
    // SUBSCRIBE, and every client subscribes to its feeds on connect — so register here too. Idempotent
    // (guarded on a known session), so the many per-session subscribes don't re-broadcast.
    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        register(accessor.getUser(), accessor.getSessionId());
    }

    private void register(Principal principal, String sessionId) {
        if (principal == null || sessionId == null || sessions.containsKey(sessionId)) {
            return;
        }
        try {
            Long userId = userApi.getCheckByGoogleId(principal.getName()).getId();
            boolean wasOnline = sessions.containsValue(userId);
            sessions.put(sessionId, userId);
            if (!wasOnline) {
                broadcast(userId, true); // offline -> online: tell their friends
            }
        } catch (Exception e) {
            log.warn("Presence register failed: {}", e.getMessage());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long userId = sessions.remove(event.getSessionId());
        if (userId == null) {
            return;
        }
        // Only flip to offline if they haven't reconnected (e.g. a refresh / second tab) within the grace.
        scheduler.schedule(() -> {
            if (!sessions.containsValue(userId)) {
                broadcast(userId, false);
            }
        }, OFFLINE_GRACE_MS, TimeUnit.MILLISECONDS);
    }

    public boolean isOnline(Long userId) {
        return sessions.containsValue(userId);
    }

    /** The subset of a user's accepted friends who are currently online — seeds the client on load. */
    public List<Long> getOnlineFriendIds(Long userId) {
        List<Long> online = new ArrayList<>();
        for (Friendship friendship : friendshipApi.getAccepted(userId)) {
            Long friendId = friendId(friendship, userId);
            if (isOnline(friendId)) {
                online.add(friendId);
            }
        }
        return online;
    }

    // Push this user's new presence to each accepted friend's personal queue.
    private void broadcast(Long userId, boolean online) {
        PresenceData data = ConversionHelper.toPresenceData(userId, online);
        for (Friendship friendship : friendshipApi.getAccepted(userId)) {
            try {
                String friendGoogleId = userApi.getCheckById(friendId(friendship, userId)).getGoogleId();
                presencePublisher.publish(friendGoogleId, data);
            } catch (Exception e) {
                log.warn("Presence broadcast to a friend failed: {}", e.getMessage());
            }
        }
    }

    private Long friendId(Friendship friendship, Long userId) {
        return friendship.getRequesterId().equals(userId)
                ? friendship.getAddresseeId()
                : friendship.getRequesterId();
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
