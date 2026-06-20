package com.coduel.redis;

import com.coduel.interfaces.MatchInvitation;
import com.coduel.model.data.NotificationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RedisMatchInvitation implements MatchInvitation {

    // One HASH per invitee (field = roomId, value = invite JSON). The whole key carries a TTL so
    // invites self-expire — "non-expired" comes for free without a cleanup job. A room invite is
    // also rejected at join() if the room is no longer OPEN, so a stale entry is harmless.
    private static final String PREFIX = "invite:user:";
    private static final Duration TTL = Duration.ofHours(1);

    // Boot 4 ships Jackson 3 only — serialize the payload ourselves (same as the WS publishers).
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private StringRedisTemplate redis;

    @Override
    public void addInvite(String googleId, NotificationData invite) {
        String key = PREFIX + googleId;
        redis.opsForHash().put(key, invite.getRoomId().toString(), JSON.writeValueAsString(invite));
        redis.expire(key, TTL);
    }

    @Override
    public List<NotificationData> getInvites(String googleId) {
        Map<Object, Object> entries = redis.opsForHash().entries(PREFIX + googleId);
        List<NotificationData> invites = new ArrayList<>();
        for (Object value : entries.values()) {
            invites.add(JSON.readValue((String) value, NotificationData.class));
        }
        return invites;
    }

    @Override
    public void removeInvite(String googleId, Long roomId) {
        redis.opsForHash().delete(PREFIX + googleId, roomId.toString());
    }
}
