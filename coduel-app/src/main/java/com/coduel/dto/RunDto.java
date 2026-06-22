package com.coduel.dto;

import com.coduel.execution.interfaces.CodeExecutor;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.helper.ConversionHelper;
import com.coduel.interfaces.RunResultPublisher;
import com.coduel.model.data.ExecutionData;
import com.coduel.model.message.RunTask;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Executes an async run and pushes the result back to its requester. The message-driven counterpart
 * to the synchronous path that used to block in CodeExecutionDto — same CodeExecutor, but the result
 * is delivered over /user/queue/run-result instead of an HTTP response. No persistence (ephemeral).
 */
@Component
@Log4j2
public class RunDto {

    @Autowired
    private CodeExecutor codeExecutor;
    @Autowired
    private RunResultPublisher runResultPublisher;

    public void run(RunTask task) {
        int totalTests = task.getTestCases() == null ? 0 : task.getTestCases().size();
        ExecutionData result;
        try {
            ExecResponse response = codeExecutor.run(ConversionHelper.toExecRequest(task));
            result = ConversionHelper.convert(response, totalTests);
        } catch (Exception e) {
            // The run already returned 202; there's no request to fail — surface it as a result so the
            // editor doesn't hang waiting forever.
            log.error("Run {} failed during execution", task.getRunId(), e);
            result = ConversionHelper.toFailedRunResult(totalTests);
        }
        result.setRunId(task.getRunId());
        runResultPublisher.publish(task.getGoogleId(), result);
    }
}
