package com.coduel.common.exception;

import com.coduel.common.constant.ApiStatus;
import com.coduel.common.data.ErrorData;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorData> handleApi(ApiException ex) {
        log.debug("Rejected [{}]: {}", ex.getStatus(), ex.getMessage());
        return build(ex.getStatus().getHttpStatus(), ex.getStatus().name(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorData> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, ApiStatus.BAD_DATA.name(), detail);
    }

    // HttpMessageNotReadableException (malformed/unbindable body) does NOT implement ErrorResponse, so handle it explicitly.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorData> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.BAD_REQUEST, ApiStatus.BAD_DATA.name(), "Malformed or unreadable request body");
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorData> handleUnknown(Throwable ex) {
        // Other Spring MVC exceptions (404/405/...) implement ErrorResponse and carry the right status.
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            String detail = er.getBody().getDetail();
            return build(status, status.name(), detail != null ? detail : status.getReasonPhrase());
        }
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ApiStatus.UNKNOWN_ERROR.name(), "Something went wrong");
    }

    private ResponseEntity<ErrorData> build(HttpStatus httpStatus, String status, String message) {
        return ResponseEntity.status(httpStatus).body(new ErrorData(status, message));
    }
}
