package com.coduel.model.message;

import com.coduel.execution.model.constant.Language;
import com.coduel.model.form.TestCaseForm;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * The payload queued for an async "run" (the editor's Run button). Unlike a submission — where the
 * queue carries just the id and the worker loads everything from the DB — a run is ephemeral and
 * never persisted, so the whole request travels in the message: who asked (googleId, for routing the
 * result back over /user/queue/run-result), what to run, and the already-clamped timeout.
 */
@Getter
@Setter
public class RunTask {

    private String runId;
    private String googleId;
    private Language language;
    private String code;
    private List<TestCaseForm> testCases;
    private long timeoutMs;
}
