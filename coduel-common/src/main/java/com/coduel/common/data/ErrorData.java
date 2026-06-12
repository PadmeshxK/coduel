package com.coduel.common.data;

import lombok.Getter;

@Getter
public class ErrorData {

    private final String status;
    private final String message;

    public ErrorData(String status, String message) {
        this.status = status;
        this.message = message;
    }
}
