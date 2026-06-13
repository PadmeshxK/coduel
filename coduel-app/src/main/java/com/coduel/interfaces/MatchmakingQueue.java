package com.coduel.interfaces;

/**
 * Port: the pool of players waiting to be matched. The Dto depends on this, not on Redis.
 */
public interface MatchmakingQueue {

    void enqueue(Long userId);

    // Atomically removes and returns one waiting player, or null if none are waiting.
    Long poll();
}
