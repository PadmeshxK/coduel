package com.coduel.common.exception;

import com.coduel.common.constant.ApiStatus;
import lombok.Getter;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;

@Getter
public class ApiException extends Exception {

    private final ApiStatus status;
    private final transient List<Object> args;

    public ApiException(ApiStatus status, Enum<?> error, List<Object> args) {
        super(format(error, args));
        this.status = status;
        this.args = args;
    }

    private static String format(Enum<?> error, List<Object> args) {
        Object[] arr = args == null ? new Object[0] : args.toArray();
        return MessageFormatter.arrayFormat(error.toString(), arr).getMessage();
    }
}
