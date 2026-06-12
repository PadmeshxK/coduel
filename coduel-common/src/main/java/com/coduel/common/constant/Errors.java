package com.coduel.common.constant;

public enum Errors {

    // ===== generic =====
    ERR_NULL("{} must not be null"),
    ERR_VALIDATION("Invalid request: {}"),
    ERR_TRIM_FAILED("Failed to sanitize field {}");

    private final String message;

    Errors(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
