package com.coduel.interfaces;

import com.coduel.model.data.PinEventData;

/** Port for delivering a live pin/unpin to the other participant (by googleId principal). */
public interface PinPublisher {

    void publish(String googleId, PinEventData event);
}
