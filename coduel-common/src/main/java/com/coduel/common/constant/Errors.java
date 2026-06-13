package com.coduel.common.constant;

public enum Errors {

    // ===== generic (001–099) =====
    ERR_001("{} must not be null"),
    ERR_002("Invalid request: {}"),
    ERR_003("Failed to sanitize field {}");

    private final String message;

    Errors(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
