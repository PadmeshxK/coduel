package com.coduel.interfaces;

import com.coduel.model.data.MatchEventData;

/**
 * Port: pushes a live event to everyone watching a match. The transport (WebSocket/STOMP)
 * lives behind this — callers (JudgeDto) don't know how it's delivered.
 */
public interface MatchEventPublisher {

    void publish(Long matchId, MatchEventData event);
}
