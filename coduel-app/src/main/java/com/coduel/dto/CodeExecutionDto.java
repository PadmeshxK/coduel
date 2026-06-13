package com.coduel.dto;

import com.coduel.common.dto.AbstractDto;
import com.coduel.model.constant.Errors;
import com.coduel.common.exception.ApiException;
import com.coduel.common.constant.ApiStatus;
import com.coduel.config.AppProperties;
import com.coduel.execution.interfaces.CodeExecutor;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.helper.ConversionHelper;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.form.ExecutionForm;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

@Component
@Log4j2
public class CodeExecutionDto extends AbstractDto {

    @Autowired
    private CodeExecutor codeExecutor;
    @Autowired
    private ExecutorService executionExecutor;
    @Autowired
    private AppProperties properties;

    public ExecutionData executeCode(ExecutionForm form) throws ApiException {
        checkValid(form);
        trim(form);
        ExecRequest request = ConversionHelper.convert(form, clampTimeout(form.getTimeoutMs()));
        try {
            Future<ExecResponse> future = executionExecutor.submit(() -> codeExecutor.run(request));
            return ConversionHelper.convert(future.get());
        } catch (RejectedExecutionException e) {
            log.warn("Execution rejected: pool and queue are full");
            throw new ApiException(ApiStatus.SERVER_BUSY, Errors.ERR_102, List.of());
        } catch (ExecutionException e) {
            return onFailure(e.getCause(), form);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(ApiStatus.UNKNOWN_ERROR, Errors.ERR_101, List.of("interrupted"));
        }
    }

    private ExecutionData onFailure(Throwable cause, ExecutionForm form) throws ApiException {
        if (cause instanceof IllegalArgumentException) {
            throw new ApiException(ApiStatus.BAD_DATA, Errors.ERR_100, List.of(form.getLanguage()));
        }
        log.error("Execution failed for language {}", form.getLanguage(), cause);
        throw new ApiException(ApiStatus.UNKNOWN_ERROR, Errors.ERR_101,
                List.of(cause == null ? "unknown" : cause.getMessage()));
    }

    private long clampTimeout(Long requestedMs) {
        if (requestedMs == null) {
            return properties.getDefaultTimeoutMs();
        }
        return Math.clamp(requestedMs, 1, properties.getMaxTimeoutMs());
    }
}
