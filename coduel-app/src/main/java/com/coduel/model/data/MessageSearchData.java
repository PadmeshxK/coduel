package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** A message-search hit — the match plus the thread it lives in, so the client can open it. */
@Getter
@Setter
public class MessageSearchData {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String snippet;
    private String kind;
    private Long createdAtMs;
    // The other participant of the thread the hit belongs to (who to open the conversation with).
    private Long otherUserId;
    private String otherDisplayName;
    private String otherAvatarUrl;
}
