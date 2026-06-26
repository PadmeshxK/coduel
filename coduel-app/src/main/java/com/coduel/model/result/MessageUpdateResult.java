package com.coduel.model.result;

import com.coduel.model.data.MessageUpdateData;
import lombok.Getter;
import lombok.Setter;

/** Who to notify (the other participant) + the edit/delete payload, so the Dto can push it live. */
@Getter
@Setter
public class MessageUpdateResult {

    private String otherGoogleId;
    private MessageUpdateData update;
}
