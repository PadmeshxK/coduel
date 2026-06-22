package com.coduel.model.data;

import com.coduel.model.constant.Verdict;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionData {

    // Correlates an async run result to the run the client is waiting on (set when pushed over
    // /user/queue/run-result; null otherwise).
    private String runId;

    private Verdict verdict;
    private Integer passedTests;
    private int totalTests;
    private long durationMs; // summed across the test cases that ran

    // First-failure detail (mirrors the judge result). compilerLogs is set only for COMPILE_ERROR;
    // failedInput/expectedOutput pinpoint the failing case; stdout/stderr are that run's output
    // (or the final run's output when everything passed).
    private String stdout;
    private String stderr;
    private String failedInput;
    private String expectedOutput;
    private String compilerLogs;
}
