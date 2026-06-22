package com.coduel.interfaces;

import com.coduel.model.data.NotificationData;

/**
 * Port: pushes a realtime notification to a specific user. The transport (STOMP user-destination)
 * lives behind this — callers don't know how it's delivered.
 */
public interface UserNotificationPublisher {

    // googleId is the STOMP principal name — matches the user-destination routing key.
    void publish(String googleId, NotificationData notification);
}
