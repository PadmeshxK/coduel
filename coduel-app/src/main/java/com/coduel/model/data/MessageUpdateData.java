package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Pushed on /user/queue/dm-update when a message is edited or deleted. The client merges these fields
 * into the existing message (keeping its reactions/quote). deleted=true → render a tombstone.
 */
@Getter
@Setter
public class MessageUpdateData {

    private Long conversationId;
    private Long messageId;
    private String body;
    private Long editedAtMs;
    private boolean deleted;
}
