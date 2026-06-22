package com.coduel.dto;

import com.coduel.common.dto.AbstractDto;
import com.coduel.common.exception.ApiException;
import com.coduel.config.AppProperties;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.RunDispatcher;
import com.coduel.model.data.RunAcceptedData;
import com.coduel.model.form.ExecutionForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CodeExecutionDto extends AbstractDto {

    @Autowired
    private RunDispatcher runDispatcher;
    @Autowired
    private AppProperties properties;

    // Validate + queue the run; a worker executes it and pushes the result to /user/queue/run-result.
    // The web thread returns immediately (no blocking on execution) — the broker is the backpressure.
    public RunAcceptedData executeCode(ExecutionForm form, String googleId) throws ApiException {
        checkValid(form);
        trim(form);
        String runId = UUID.randomUUID().toString();
        runDispatcher.dispatch(
                ConversionHelper.toRunTask(runId, googleId, form, clampTimeout(form.getTimeoutMs())));
        return ConversionHelper.toRunAcceptedData(runId);
    }

    private long clampTimeout(Long requestedMs) {
        if (requestedMs == null) {
            return properties.getDefaultTimeoutMs();
        }
        return Math.clamp(requestedMs, 1, properties.getMaxTimeoutMs());
    }
}
