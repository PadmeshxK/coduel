package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** An ephemeral lobby-chat message — kept in a Redis ring buffer (not the DB) and broadcast live. */
@Getter
@Setter
public class RoomChatData {

    private Long senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String body;
    private Long createdAtMs;
}
