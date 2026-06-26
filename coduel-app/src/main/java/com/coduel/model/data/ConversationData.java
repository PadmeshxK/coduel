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
    // The OTHER participant's read marker (epoch ms) — seeds the live "Seen … " receipt on load.
    private Long otherLastReadAtMs;
    // The viewer's personalization for this thread (defaults when never customized): a private nickname
    // for the other person, a per-DM accent for the row, and whether they've muted it.
    private String nickname;
    private String accentHex;
    private boolean muted;
}
