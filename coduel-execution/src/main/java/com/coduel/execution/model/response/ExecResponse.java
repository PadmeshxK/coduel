package com.coduel.execution.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExecResponse {

    private String stdout;
    private String stderr;
    private int exitCode;
    private boolean timedOut;
    private long durationMs;

}
