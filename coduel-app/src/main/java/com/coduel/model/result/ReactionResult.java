package com.coduel.model.result;

import com.coduel.model.data.ReactionEventData;
import lombok.Getter;
import lombok.Setter;

/** Who to notify (the other participant) + the event payload, so the Dto can push the live update. */
@Getter
@Setter
public class ReactionResult {

    private String otherGoogleId;
    private ReactionEventData event;
}
