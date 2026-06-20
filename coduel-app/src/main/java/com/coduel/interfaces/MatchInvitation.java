package com.coduel.interfaces;

import com.coduel.model.data.NotificationData;

import java.util.List;

/**
 * Port: short-lived storage of room invites so a user can still see them after a reload or after
 * being offline (the live STOMP push is fire-and-forget). Invites are ephemeral and time-bounded —
 * the impl is expected to expire them automatically. Keyed by the invitee's googleId (the STOMP
 * principal), so callers never need to resolve a userId.
 */
public interface MatchInvitation {

    // Store (or refresh) a pending invite for the invitee.
    void addInvite(String googleId, NotificationData invite);

    // All non-expired invites for the invitee (empty if none).
    List<NotificationData> getInvites(String googleId);

    // Drop one invite once it's accepted or declined. No-op if absent.
    void removeInvite(String googleId, Long roomId);
}
