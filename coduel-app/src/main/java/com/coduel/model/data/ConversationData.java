package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** A row in the DM inbox — the other party + a preview of the latest message. */
@Getter
@Setter
public class ConversationData {

    private Long conversationId;
    private Long otherUserId;
    private String otherDisplayName;
    private String otherAvatarUrl;
    private String lastPreview;
    private Long lastMessageAtMs;
    private Long lastSenderId;
    // True when the viewer has a newer message from the other side than their read marker.
    private boolean unread;
}
