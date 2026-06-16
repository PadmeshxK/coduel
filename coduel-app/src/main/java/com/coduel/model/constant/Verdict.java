package com.coduel.model.constant;

public enum Verdict {

    PENDING,
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILE_ERROR,
    // Judging itself failed (worker error exhausted retries, or never ran) — not the user's fault.
    INTERNAL_ERROR
}
