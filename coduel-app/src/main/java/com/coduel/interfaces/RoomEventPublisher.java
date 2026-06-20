package com.coduel.interfaces;

import com.coduel.model.data.RoomEventData;

/**
 * Port: pushes a lobby event to everyone in a room (/topic/room/{roomId}). Distinct from the match
 * topic — a room outlives its matches, so its roster/start/close events live on their own channel.
 */
public interface RoomEventPublisher {

    void publish(Long roomId, RoomEventData event);
}
