package com.coduel.interfaces;

import com.coduel.model.data.NotificationData;

import java.util.List;

/**
 * Port: a user's inbox of actionable notifications (room invites, duel challenges, …) so they still
 * surface after a reload or while offline — the live STOMP push ({@link UserNotificationPublisher})
 * is fire-and-forget, this is the copy left behind. One inbox for every kind: the {@code type} on
 * {@link NotificationData} is the discriminator, not a separate interface per type. Keyed by the
 * recipient's googleId (the STOMP principal). Entries are ephemeral — each carries its own
 * {@code expiresAtMs} and the impl reaps them automatically.
 */
public interface NotificationInbox {

    // Add (or refresh) a notification for the recipient. The hash field is the notification's id.
    void add(String googleId, NotificationData notification);

    // All non-expired notifications for the recipient (empty if none).
    List<NotificationData> getAll(String googleId);

    // One notification by id, or null if absent/expired.
    NotificationData get(String googleId, String id);

    // Drop one notification once it's acted on. No-op if absent.
    void remove(String googleId, String id);
}
