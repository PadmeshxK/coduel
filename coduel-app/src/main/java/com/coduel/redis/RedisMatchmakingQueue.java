package com.coduel.redis;

import com.coduel.interfaces.MatchmakingQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMatchmakingQueue implements MatchmakingQueue {

    private static final String QUEUE_KEY = "matchmaking:queue:duel";

    @Autowired
    private StringRedisTemplate redis;

    @Override
    public void enqueue(Long userId) {
        // Drop any stale entry first so a user appears at most once — re-clicking "find match" (or a
        // race between status + join) must not stack the same userId in the queue twice.
        redis.opsForList().remove(QUEUE_KEY, 0, userId.toString());
        redis.opsForList().leftPush(QUEUE_KEY, userId.toString());
    }

    @Override
    public Long poll() {
        // RPOP is atomic — two concurrent polls can't grab the same waiting player.
        String userId = redis.opsForList().rightPop(QUEUE_KEY);
        return userId == null ? null : Long.valueOf(userId);
    }

    @Override
    public void remove(Long userId) {
        // count = 0 -> remove every occurrence of this user from the list.
        redis.opsForList().remove(QUEUE_KEY, 0, userId.toString());
    }
}
