package com.coduel.execution.impl;

import com.coduel.execution.model.constant.ExecutionVerdict;
import com.coduel.execution.model.constant.Language;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.request.TestCase;
import com.coduel.execution.model.response.ExecResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessBuilderExecutorTest {

    private final ProcessBuilderExecutor executor = new ProcessBuilderExecutor();

    @Test
    void pythonHelloWorldShouldPrintToStdout() {
        ExecResponse response = run("print('hello')", null, Duration.ofSeconds(5));

        assertEquals(ExecutionVerdict.ACCEPTED, response.getVerdict());
        assertEquals("hello", response.getStdout().trim());
    }

    @Test
    void shouldFeedStdinToProgram() {
        ExecResponse response = run("print(input())", "ping", Duration.ofSeconds(5));

        assertEquals(ExecutionVerdict.ACCEPTED, response.getVerdict());
        assertEquals("ping", response.getStdout().trim());
    }

    @Test
    void infiniteLoopShouldTimeOut() {
        ExecResponse response = run("while True: pass", null, Duration.ofSeconds(1));

        assertEquals(ExecutionVerdict.TIME_LIMIT_EXCEEDED, response.getVerdict());
    }

    @Test
    void runtimeErrorShouldReturnNonZeroExit() {
        ExecResponse response = run("raise ValueError('boom')", null, Duration.ofSeconds(5));

        assertEquals(ExecutionVerdict.RUNTIME_ERROR, response.getVerdict());
        assertTrue(response.getStderr().contains("ValueError"));
    }

    // Single-test-case request with a blank expected output, so evaluate() just runs the program
    // (no answer comparison) — these tests check raw executor behaviour, not answer-checking.
    private ExecResponse run(String code, String stdin, Duration timeout) {
        TestCase testCase = new TestCase();
        testCase.setInput(stdin);
        testCase.setExpectedOutput("");

        ExecRequest request = new ExecRequest();
        request.setLanguage(Language.PYTHON);
        request.setCode(code);
        request.setTestCases(List.of(testCase));
        request.setTimeout(timeout);
        return executor.run(request);
    }
}
