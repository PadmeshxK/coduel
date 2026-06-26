package com.coduel.interfaces;

import com.coduel.model.data.ReactionEventData;

/** Port for delivering a live message-reaction update to the other participant (by googleId principal). */
public interface ReactionPublisher {

    void publish(String googleId, ReactionEventData event);
}
