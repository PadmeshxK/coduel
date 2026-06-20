package com.coduel.model.data;

import com.coduel.model.constant.NotificationEventType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationData {

    private NotificationEventType type;

    // ROOM_INVITE — the room to join.
    private Long roomId;
    // FRIEND_REQUEST — the request to accept/decline.
    private Long requestId;
    // Common: who triggered it, and when (millis) for recency ordering in the notification center.
    private Long fromUserId;
    private String fromDisplayName;
    private String fromAvatarUrl;
    private Long createdAtMs;
}
