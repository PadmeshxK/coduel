package com.coduel.model.result;

import com.coduel.model.data.PinEventData;
import lombok.Getter;
import lombok.Setter;

/** Who to notify (the other participant) + the pin/unpin event, so the Dto can push it live. */
@Getter
@Setter
public class PinResult {

    private String otherGoogleId;
    private PinEventData event;
}
