package com.coduel.interfaces;

import com.coduel.model.data.MatchEventData;

/**
 * Port: pushes a live event to everyone watching a match (/topic/match/{id}). The transport
 * (WebSocket/STOMP) lives behind this — callers don't know how it's delivered.
 */
public interface MatchEventPublisher {

    void publish(Long matchId, MatchEventData event);
}
