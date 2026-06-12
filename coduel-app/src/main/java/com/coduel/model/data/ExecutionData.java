package com.coduel.model.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecutionData {

    private String stdout;
    private String stderr;
    private int exitCode;
    private boolean timedOut;
    private long durationMs;
}
