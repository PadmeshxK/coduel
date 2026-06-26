package com.coduel.model.constant;

public enum NotificationEventType {

    ROOM_INVITE,
    FRIEND_REQUEST,
    // Live-only signal pushed to the original requester when their request is accepted. Not an
    // actionable pending item (never returned by GET /notification) — purely a "you're now friends" cue.
    FRIEND_ACCEPTED,
    // Live-only, silent signal to the requester when their request is declined — the UI reverts the
    // "Requested" state to "Add". No toast (a rejection drops quietly).
    FRIEND_DECLINED,
    // A friend wants to duel you — actionable (accept/decline), carries challengeId.
    DUEL_CHALLENGE,
    // A challenge was accepted — carries matchId; both players navigate into the duel.
    CHALLENGE_ACCEPTED,
    // A challenge you sent was declined — a quiet "they passed" cue.
    CHALLENGE_DECLINED,
    // The challenger withdrew a pending challenge — live signal to the target so its popup/bell row
    // drops (the inbox entry is removed server-side too); carries challengeId.
    CHALLENGE_WITHDRAWN,
    // Ranked matchmaking paired you — carries matchId; both players navigate into the duel (no polling).
    MATCHMAKING_FOUND,
    // A friend sent you a direct message — a toast cue; the conversation itself lives on the Messages page.
    DM_RECEIVED
}
