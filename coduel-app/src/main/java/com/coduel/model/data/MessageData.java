package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** A single DM — returned in a thread page and pushed live over /user/queue/dm. */
@Getter
@Setter
public class MessageData {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String body;
    private Long createdAtMs;
}
