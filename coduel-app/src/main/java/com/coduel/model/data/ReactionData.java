package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

/** A single reaction on a message (embedded in MessageData) — who reacted and with what. */
@Getter
@Setter
public class ReactionData {

    private Long userId;
    private String emoji;
}
