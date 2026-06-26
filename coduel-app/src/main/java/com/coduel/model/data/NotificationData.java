package com.coduel.model.data;

import com.coduel.model.constant.NotificationEventType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationData {

    private NotificationEventType type;

    // Stable per-notification key for the NotificationStore (Redis hash field). Reconstructable from
    // the business id so removal stays O(1): challenge -> challengeId, room invite -> "room:"+roomId.
    private String id;
    // Logical expiry (millis). The store key has a coarse reaper TTL; reads drop anything past this,
    // so notifications with different lifetimes (90s challenge, 1h invite) can share one store.
    private Long expiresAtMs;

    // ROOM_INVITE — the room to join.
    private Long roomId;
    // FRIEND_REQUEST — the request to accept/decline.
    private Long requestId;
    // DUEL_CHALLENGE — the challenge to accept/decline.
    private String challengeId;
    // DUEL_CHALLENGE — optional specific problem to duel on (a shared-problem challenge); null = random.
    private String problemSlug;
    // CHALLENGE_ACCEPTED — the duel match to jump into.
    private Long matchId;
    // DM_RECEIVED — the kind of message ("TEXT"/"IMAGE"/"CODE"/"PROBLEM_SHARE"), so the toast/bell can
    // say what they sent ("sent you an image", "shared a problem", …).
    private String messageKind;
    // Common: who triggered it, and when (millis) for recency ordering in the notification center.
    private Long fromUserId;
    private String fromDisplayName;
    private String fromAvatarUrl;
    private Long createdAtMs;
}
