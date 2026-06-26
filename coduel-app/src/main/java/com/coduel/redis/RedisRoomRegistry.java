package com.coduel.redis;

import com.coduel.entity.Room;
import com.coduel.interfaces.RoomRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/**
 * The room aggregate as one JSON value per room (`room:{id}`) with a sliding TTL — refreshed on every
 * save AND read, so an actively-used lobby stays alive while an abandoned one self-cleans. This is why
 * rooms no longer accumulate in (or get orphaned by a restart of) durable storage.
 */
@Component
public class RedisRoomRegistry implements RoomRegistry {

    private static final String KEY = "room:";
    private static final String SEQ = "room:id:seq";
    private static final Duration TTL = Duration.ofHours(6);
    // Boot 4 ships Jackson 3 only — serialize ourselves (same as the other Redis stores / publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private StringRedisTemplate redis;

    @Override
    public Long nextId() {
        // Atomic, monotonic — INCR on a fresh key yields 1, then 2, 3, … so each room gets a unique id.
        return redis.opsForValue().increment(SEQ);
    }

    @Override
    public Room get(Long id) {
        String key = KEY + id;
        String raw = redis.opsForValue().get(key);
        if (raw == null) {
            return null;
        }
        redis.expire(key, TTL); // sliding: keep an actively-viewed room from expiring under its members
        return JSON.readValue(raw, Room.class);
    }

    @Override
    public void save(Room room) {
        redis.opsForValue().set(KEY + room.getId(), JSON.writeValueAsString(room), TTL);
    }

    @Override
    public void delete(Long id) {
        redis.delete(KEY + id);
    }
}
