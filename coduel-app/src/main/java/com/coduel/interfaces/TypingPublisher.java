package com.coduel.interfaces;

import com.coduel.model.data.TypingData;

/** Port for delivering a typing signal to a specific recipient (by their googleId principal). */
public interface TypingPublisher {

    void publish(String googleId, TypingData typing);
}
