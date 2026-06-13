package com.coduel.common.constant;

import org.springframework.http.HttpStatus;

public enum ApiStatus {

    BAD_DATA(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    RESOURCE_EXISTS(HttpStatus.CONFLICT),
    AUTH_ERROR(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    SERVER_BUSY(HttpStatus.SERVICE_UNAVAILABLE),
    REMOTE_ERROR(HttpStatus.BAD_GATEWAY),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ApiStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
