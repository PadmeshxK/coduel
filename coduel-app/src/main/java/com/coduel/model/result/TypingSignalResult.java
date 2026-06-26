package com.coduel.model.result;

import com.coduel.model.data.TypingData;
import lombok.Getter;
import lombok.Setter;

/** Internal carrier: who to notify (the recipient's googleId) + the typing payload. */
@Getter
@Setter
public class TypingSignalResult {

    private String recipientGoogleId;
    private TypingData typing;
}
