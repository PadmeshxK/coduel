package com.coduel.helper;

import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.form.ExecutionForm;

import java.time.Duration;

public class ConversionHelper {

    public static ExecRequest convert(ExecutionForm form, long timeoutMs) {
        ExecRequest request = new ExecRequest();
        request.setLanguage(form.getLanguage());
        request.setCode(form.getCode());
        request.setStdin(form.getStdin());
        request.setTimeout(Duration.ofMillis(timeoutMs));
        return request;
    }

    public static ExecutionData convert(ExecResponse response) {
        ExecutionData data = new ExecutionData();
        data.setStdout(response.getStdout());
        data.setStderr(response.getStderr());
        data.setExitCode(response.getExitCode());
        data.setTimedOut(response.isTimedOut());
        data.setDurationMs(response.getDurationMs());
        return data;
    }
}
