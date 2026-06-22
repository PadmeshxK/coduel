package com.coduel.model.result;

import com.coduel.entity.Conversation;
import com.coduel.entity.User;
import lombok.Getter;
import lombok.Setter;

/** Internal carrier: a conversation paired with the resolved other participant (for the inbox list). */
@Getter
@Setter
public class ConversationView {

    private Conversation conversation;
    private User other;
    // Whether the viewing user has an unread last message in this thread.
    private boolean unread;
}
