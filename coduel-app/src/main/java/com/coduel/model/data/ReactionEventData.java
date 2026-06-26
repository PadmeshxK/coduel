package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/**
 * Pushed live to the other participant over /user/queue/dm-reaction when someone reacts to (or clears
 * their reaction from) a message. removed=true means the reaction was cleared.
 */
@Getter
@Setter
public class ReactionEventData {

    private Long conversationId;
    private Long messageId;
    private Long userId;
    private String emoji;
    private boolean removed;
}
