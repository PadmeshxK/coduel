package com.coduel.model.data;

import com.coduel.model.constant.MessageKind;
import lombok.Getter;
import lombok.Setter;

/** A pinned message as shown in the pin bar — the target + a quoted preview + who pinned it. */
@Getter
@Setter
public class PinnedMessageData {

    private Long messageId;
    private Long senderId;
    private String preview;
    private Long pinnedByUserId;
    // Lets the client render an icon + label for non-text pins.
    private MessageKind kind;
}
