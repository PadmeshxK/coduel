package com.coduel.interfaces;

import com.coduel.model.data.MessageData;

/**
 * Port: delivers a direct message to its recipient live. googleId is the STOMP principal; the message
 * also lands in the DB, so this is the real-time nudge, not the source of truth.
 */
public interface DmPublisher {

    void publish(String googleId, MessageData message);
}
