package com.coduel.interfaces;

import com.coduel.model.data.SubmissionData;

/**
 * Port: delivers a judged SOLO (practice) submission's result back to its submitter, so the page
 * doesn't have to poll. googleId is the STOMP principal; the client matches by submissionId. Duel
 * submissions don't use this — they fan out to both players on the match topic instead.
 */
public interface SubmissionResultPublisher {

    void publish(String googleId, SubmissionData result);
}
