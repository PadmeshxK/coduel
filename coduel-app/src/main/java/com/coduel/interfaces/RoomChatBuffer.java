package com.coduel.interfaces;

import com.coduel.model.data.RoomChatData;

import java.util.List;

/**
 * Port for the lobby's ephemeral chat history — a bounded, expiring ring buffer (NOT durable storage).
 * Lobby chat is throwaway: only the last N messages survive, and the whole thing TTLs out.
 */
public interface RoomChatBuffer {

    void append(Long roomId, RoomChatData message);

    /** The recent messages for a room, oldest-first (chronological). */
    List<RoomChatData> getRecent(Long roomId);
}
