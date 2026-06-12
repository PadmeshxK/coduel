package com.coduel.model.constant;

public enum Errors {

    ERR_UNSUPPORTED_LANGUAGE("Language {} is not supported"),
    ERR_EXECUTION_FAILED("Code execution failed: {}"),
    ERR_SERVER_BUSY("Server is at execution capacity; please retry shortly");

    private final String message;

    Errors(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
