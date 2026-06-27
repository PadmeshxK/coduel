package com.coduel.model.result;

import com.coduel.model.data.ReadReceiptData;
import lombok.Getter;
import lombok.Setter;

/**
 * Internal carrier for markRead: the OTHER participant's id + googleId, plus the read-receipt payload.
 * otherUserId is always present (the Dto clears that sender's DM cue from the reader's inbox); receipt
 * is null when the reader disabled read receipts (no live "Seen" push).
 */
@Getter
@Setter
public class MarkReadResult {

    private String otherGoogleId;
    private Long otherUserId;
    private ReadReceiptData receipt;
}
