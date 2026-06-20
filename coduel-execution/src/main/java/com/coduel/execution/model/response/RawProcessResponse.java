package com.coduel.execution.model.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RawProcessResponse {
    String stdout;
    String stderr;
    int exitCode;
    boolean timedOut;
    long durationMs;
}
