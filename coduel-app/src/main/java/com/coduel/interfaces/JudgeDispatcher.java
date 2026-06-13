package com.coduel.interfaces;

import com.coduel.entity.Submission;

/**
 * Port: hands a persisted submission off for asynchronous judging. The transport (Kafka vs
 * in-process) lives entirely behind this interface — SubmissionDto never knows which impl is wired.
 */
public interface JudgeDispatcher {

    void dispatch(Submission submission);
}
