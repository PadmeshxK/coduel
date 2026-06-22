package com.coduel.execution.interfaces;

import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;

public interface CodeExecutor {

    // Compile once, run the code against every test case, return one summarized verdict. Both the
    // synchronous Run (visible cases) and the async judge (all cases) go through this same path.
    ExecResponse run(ExecRequest request);
}
