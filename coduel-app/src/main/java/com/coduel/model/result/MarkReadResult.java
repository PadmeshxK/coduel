package com.coduel.model.result;

import com.coduel.model.data.ReadReceiptData;
import lombok.Getter;
import lombok.Setter;

/** Internal carrier: the OTHER participant's googleId (whom to notify) + the read-receipt payload. */
@Getter
@Setter
public class MarkReadResult {

    private String otherGoogleId;
    private ReadReceiptData receipt;
}
