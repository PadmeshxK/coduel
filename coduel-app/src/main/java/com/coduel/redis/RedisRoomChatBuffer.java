package com.coduel.redis;

import com.coduel.interfaces.RoomChatBuffer;
import com.coduel.model.data.RoomChatData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Lobby chat history as a Redis ring buffer: LPUSH newest, LTRIM to the last {@value #MAX}, EXPIRE so
 * an abandoned room's chat self-cleans. Ephemeral by design — a refresh re-hydrates from here, but
 * nothing is persisted to the DB (unlike DMs).
 */
@Component
public class RedisRoomChatBuffer implements RoomChatBuffer {

    private static final int MAX = 200;
    private static final Duration TTL = Duration.ofHours(12);
    // Boot 4 ships Jackson 3 only — serialize ourselves (same as the STOMP publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private StringRedisTemplate redis;

    private String key(Long roomId) {
        return "room:chat:" + roomId;
    }

    @Override
    public void append(Long roomId, RoomChatData message) {
        String key = key(roomId);
        redis.opsForList().leftPush(key, JSON.writeValueAsString(message));
        redis.opsForList().trim(key, 0, MAX - 1); // keep only the newest MAX
        redis.expire(key, TTL);
    }

    @Override
    public List<RoomChatData> getRecent(Long roomId) {
        List<String> raw = redis.opsForList().range(key(roomId), 0, MAX - 1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        // Stored newest-first (LPUSH) — walk backwards to return oldest-first for the client.
        List<RoomChatData> out = new ArrayList<>(raw.size());
        for (int i = raw.size() - 1; i >= 0; i--) {
            try {
                out.add(JSON.readValue(raw.get(i), RoomChatData.class));
            } catch (Exception ignored) {
                // skip a malformed entry rather than failing the whole hydrate
            }
        }
        return out;
    }
}
