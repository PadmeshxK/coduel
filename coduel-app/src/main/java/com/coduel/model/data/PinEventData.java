package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Pushed on /user/queue/dm-pin when a message is pinned/unpinned. pinned=true carries the pin payload
 * (the client adds it); pinned=false means remove the pin for messageId.
 */
@Getter
@Setter
public class PinEventData {

    private Long conversationId;
    private Long messageId;
    private boolean pinned;
    private PinnedMessageData pin;
}
