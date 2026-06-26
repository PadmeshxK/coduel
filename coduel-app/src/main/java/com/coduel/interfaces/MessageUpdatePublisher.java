package com.coduel.interfaces;

import com.coduel.model.data.MessageUpdateData;

/** Port for delivering a live message edit/delete to the other participant (by googleId principal). */
public interface MessageUpdatePublisher {

    void publish(String googleId, MessageUpdateData update);
}
