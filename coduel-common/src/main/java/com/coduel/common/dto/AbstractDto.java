package com.coduel.common.dto;

import com.coduel.common.annotation.NoTrim;
import com.coduel.common.constant.Errors;
import com.coduel.common.exception.ApiException;
import com.coduel.common.constant.ApiStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public abstract class AbstractDto {

    @Autowired
    private Validator validator;

    /** Bean Validation on the form; collects all violations into one BAD_DATA error. */
    protected <T> void checkValid(T form) throws ApiException {
        Set<ConstraintViolation<T>> violations = validator.validate(form);
        if (violations.isEmpty()) {
            return;
        }
        String detail = violations.stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_002, List.of(detail));
    }

    /** Trim every non-null, non-@NoTrim String field on the form, in place. */
    protected void trim(Object form) throws ApiException {
        if (form == null) {
            return;
        }
        for (Field field : form.getClass().getDeclaredFields()) {
            if (!String.class.equals(field.getType()) || field.isAnnotationPresent(NoTrim.class)) {
                continue;
            }
            field.setAccessible(true);
            try {
                String value = (String) field.get(form);
                if (value != null) {
                    field.set(form, value.trim());
                }
            } catch (IllegalAccessException e) {
                log.error("Failed to trim {} on {}", field.getName(), form.getClass().getSimpleName(), e);
                throw new ApiException(ApiStatus.UNKNOWN_ERROR, Errors.ERR_003, List.of(field.getName()));
            }
        }
    }
}
