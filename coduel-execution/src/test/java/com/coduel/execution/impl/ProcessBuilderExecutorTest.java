package com.coduel.execution.impl;

import com.coduel.execution.model.constant.Language;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessBuilderExecutorTest {

    private final ProcessBuilderExecutor executor = new ProcessBuilderExecutor();

    @Test
    void pythonHelloWorldShouldPrintToStdout() {
        ExecResponse response = executor.run(request("print('hello')", null, Duration.ofSeconds(5)));

        assertEquals(0, response.getExitCode());
        assertEquals("hello", response.getStdout().trim());
        assertFalse(response.isTimedOut());
    }

    @Test
    void shouldFeedStdinToProgram() {
        ExecResponse response = executor.run(request("print(input())", "ping", Duration.ofSeconds(5)));

        assertEquals(0, response.getExitCode());
        assertEquals("ping", response.getStdout().trim());
        assertFalse(response.isTimedOut());
    }

    @Test
    void infiniteLoopShouldTimeOut() {
        ExecResponse response = executor.run(request("while True: pass", null, Duration.ofSeconds(1)));

        assertTrue(response.isTimedOut());
    }

    @Test
    void runtimeErrorShouldReturnNonZeroExit() {
        ExecResponse response = executor.run(request("raise ValueError('boom')", null, Duration.ofSeconds(5)));

        assertNotEquals(0, response.getExitCode());
        assertTrue(response.getStderr().contains("ValueError"));
        assertFalse(response.isTimedOut());
    }

    private ExecRequest request(String code, String stdin, Duration timeout) {
        ExecRequest request = new ExecRequest();
        request.setLanguage(Language.PYTHON);
        request.setCode(code);
        request.setStdin(stdin);
        request.setTimeout(timeout);
        return request;
    }
}
