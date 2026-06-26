package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** Pushed to the OTHER participant when someone reads a DM thread — drives the live "Seen" receipt. */
@Getter
@Setter
public class ReadReceiptData {

    private Long conversationId;
    private Long readerUserId;
    private Long readAtMs;
}
