package com.coduel.interfaces;

import com.coduel.model.data.ExecutionData;

/**
 * Port: delivers a finished run's result back to the one user who requested it. googleId is the STOMP
 * principal; the {@code runId} on the result lets the client match it to the run it's waiting on.
 */
public interface RunResultPublisher {

    void publish(String googleId, ExecutionData result);
}
