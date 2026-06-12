package com.coduel.execution.interfaces;

import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;

public interface CodeExecutor {
    public ExecResponse run(ExecRequest request);
}
