package com.coduel.execution.model.response;

import com.coduel.execution.model.constant.ExecutionVerdict;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecResponse {
    private ExecutionVerdict verdict;
    private int passedTests;
    private long durationMs; // Max runtime across all test cases

    private String compilerLogs; // Set if COMPILE_ERROR

    // Actual outputs of the run that stopped execution (or the final run if all pass)
    private String stdout;
    private String stderr;

    // The inputs/expected outputs that caused the failure (null if ACCEPTED)
    private String failedInput;
    private String expectedOutput;
}
