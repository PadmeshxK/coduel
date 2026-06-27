package com.coduel.redis;

import com.coduel.interfaces.NotificationInbox;
import com.coduel.model.data.NotificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RedisNotificationInbox implements NotificationInbox {

    // One HASH per recipient (field = notification id, value = JSON). The key carries a generous reaper
    // TTL (refreshed on each add) purely so abandoned inboxes don't live forever — actual lifetime is
    // governed per-entry: time-boxed offers carry expiresAtMs (90s challenge, 1h invite) dropped on
    // read, while DMs / friend requests have NO expiry and persist until they're read / acted on.
    private static final String PREFIX = "notif:user:";
    private static final Duration REAPER_TTL = Duration.ofDays(30);

    // Boot 4 ships Jackson 3 only — serialize the payload ourselves (same as the WS publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private StringRedisTemplate redis;

    @Override
    public void add(String googleId, NotificationData notification) {
        String key = PREFIX + googleId;
        redis.opsForHash().put(key, notification.getId(), JSON.writeValueAsString(notification));
        redis.expire(key, REAPER_TTL);
    }

    @Override
    public List<NotificationData> getAll(String googleId) {
        Map<Object, Object> entries = redis.opsForHash().entries(PREFIX + googleId);
        List<NotificationData> live = new ArrayList<>();
        for (Object value : entries.values()) {
            NotificationData n = JSON.readValue((String) value, NotificationData.class);
            if (!isExpired(n)) {
                live.add(n);
            }
        }
        return live;
    }

    @Override
    public NotificationData get(String googleId, String id) {
        Object value = redis.opsForHash().get(PREFIX + googleId, id);
        if (value == null) {
            return null;
        }
        NotificationData n = JSON.readValue((String) value, NotificationData.class);
        return isExpired(n) ? null : n;
    }

    @Override
    public void remove(String googleId, String id) {
        redis.opsForHash().delete(PREFIX + googleId, id);
    }

    @Override
    public boolean removeIfPresent(String googleId, String id) {
        // HDEL returns the number of fields actually removed — 1 only for the caller that wins the race.
        Long removed = redis.opsForHash().delete(PREFIX + googleId, id);
        return removed != null && removed > 0;
    }

    private static boolean isExpired(NotificationData n) {
        return n.getExpiresAtMs() != null && Instant.now().toEpochMilli() > n.getExpiresAtMs();
    }
}
