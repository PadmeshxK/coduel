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

    // One HASH per recipient (field = notification id, value = JSON). The key carries a coarse reaper
    // TTL so nothing lingers forever; per-notification expiry is enforced on read via expiresAtMs, so
    // entries with different lifetimes (90s challenge, 1h invite) coexist under one key.
    private static final String PREFIX = "notif:user:";
    private static final Duration REAPER_TTL = Duration.ofHours(1);

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

    private static boolean isExpired(NotificationData n) {
        return n.getExpiresAtMs() != null && Instant.now().toEpochMilli() > n.getExpiresAtMs();
    }
}
