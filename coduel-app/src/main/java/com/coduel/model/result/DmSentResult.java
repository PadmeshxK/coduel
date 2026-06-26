package com.coduel.model.result;

import com.coduel.entity.Message;
import com.coduel.entity.User;
import lombok.Getter;
import lombok.Setter;

/**
 * Internal carrier: the persisted message, the recipient's googleId (to push it live), and the sender
 * (so the Dto can also fire a "you got a DM" notification to the recipient).
 */
@Getter
@Setter
public class DmSentResult {

    private Message message;
    private String recipientGoogleId;
    private User sender;
    // The replied-to message (null = not a reply), so the echoed/pushed payload carries the quote.
    private Message replyTo;
}
