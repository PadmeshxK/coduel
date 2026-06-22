package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** Ephemeral "X is typing" signal, pushed to the recipient over /user/queue/typing. Never persisted. */
@Getter
@Setter
public class TypingData {

    private Long fromUserId;
}
