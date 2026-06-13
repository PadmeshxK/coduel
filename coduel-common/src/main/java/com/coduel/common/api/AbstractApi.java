package com.coduel.common.api;

import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;


import java.util.List;
import java.util.Objects;

public abstract class AbstractApi {

    protected void checkNotNull(Object value, Enum<?> error, List<Object> args) throws ApiException {
        if (Objects.isNull(value)) {
            throw new ApiException(ApiStatus.BAD_DATA, error, args);
        }
    }
}
