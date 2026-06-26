package com.coduel.model.result;

import com.coduel.entity.Message;
import com.coduel.entity.MessageReaction;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Internal carrier: a message paired with its reactions (for a thread page). */
@Getter
@Setter
public class MessageView {

    private Message message;
    private List<MessageReaction> reactions;
    // The message this one replies to (null = not a reply / original gone) — for the quoted preview.
    private Message replyTo;
}
