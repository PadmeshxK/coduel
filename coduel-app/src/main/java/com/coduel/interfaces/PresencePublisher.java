package com.coduel.interfaces;

import com.coduel.model.data.PresenceData;

/** Port for delivering a presence change to a specific user (by their googleId principal). */
public interface PresencePublisher {

    void publish(String googleId, PresenceData presence);
}
