package com.coduel.execution.impl;

import com.coduel.execution.exception.CodeExecutionException;
import com.coduel.execution.helper.ConversionHelper;
import com.coduel.execution.interfaces.CodeExecutor;
import com.coduel.execution.model.config.LanguageConfig;
import com.coduel.execution.model.constant.ExecutionVerdict;
import com.coduel.execution.model.constant.Language;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.request.TestCase;
import com.coduel.execution.model.response.ExecResponse;
import com.coduel.execution.model.response.RawProcessResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ProcessBuilderExecutor implements CodeExecutor {

    private static final String STDIN_FILE = "input.txt";
    private static final String STDOUT_FILE = "stdout.txt";
    private static final String STDERR_FILE = "stderr.txt";

    // Compilation gets its own (larger) budget — g++ is far slower than a single test run, and it
    // happens once per submission, not once per test.
    private static final Duration COMPILE_TIMEOUT = Duration.ofSeconds(10);

    // Cap per stream — a runaway program (e.g. an infinite print loop) can't OOM us or balloon the
    // response. We read at most this many bytes back even if the captured file is far larger.
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    private static final Map<Language, LanguageConfig> CONFIG_MAP = ConversionHelper.constructConfig();

    // ---- judged path: one compile, then every test case, summarized into a single verdict ----

    public ExecResponse run(ExecRequest request) {
        LanguageConfig config = resolveConfig(request.getLanguage());
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("coduel-");
            writeSource(workDir, config, request.getCode());

            RawProcessResponse compile = compileIfNeeded(workDir, config);
            if (compile != null && !compiledOk(compile)) {
                return compileError(compile);
            }
            return judge(workDir, config, request);
        } catch (IOException e) {
            throw new CodeExecutionException("Failed to execute code", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag
            throw new CodeExecutionException("Execution interrupted", e);
        } finally {
            cleanup(workDir);
        }
    }

    /** Run each test case in order; stop at (and capture) the first non-ACCEPTED one. */
    private ExecResponse judge(Path workDir, LanguageConfig config, ExecRequest request)
            throws IOException, InterruptedException {
        ExecResponse response = new ExecResponse();
        int passed = 0;
        long totalMs = 0; // total time = sum of every test case's runtime
        RawProcessResponse last = null;

        for (TestCase testCase : request.getTestCases()) {
            writeStdin(workDir, testCase.getInput());
            RawProcessResponse raw = runProcess(workDir, config, request.getTimeout());
            last = raw;
            totalMs += raw.getDurationMs();

            ExecutionVerdict verdict = evaluate(raw, testCase);
            if (verdict != ExecutionVerdict.ACCEPTED) {
                response.setVerdict(verdict);
                response.setPassedTests(passed);
                response.setDurationMs(totalMs);
                response.setStdout(raw.getStdout());
                response.setStderr(raw.getStderr());
                response.setFailedInput(testCase.getInput());
                response.setExpectedOutput(testCase.getExpectedOutput());
                return response;
            }
            passed++;
        }

        response.setVerdict(ExecutionVerdict.ACCEPTED);
        response.setPassedTests(passed);
        response.setDurationMs(totalMs);
        // All passed: surface the final run's output (useful for the Run console / no-expected cases).
        if (last != null) {
            response.setStdout(last.getStdout());
            response.setStderr(last.getStderr());
        }
        return response;
    }

    /** Compare one run against its expected output (whitespace-tolerant), or classify the failure.
     *  A blank expected output means "just run it" (the Run console's scratchpad) — no comparison. */
    private ExecutionVerdict evaluate(RawProcessResponse raw, TestCase testCase) {
        if (raw.isTimedOut()) {
            return ExecutionVerdict.TIME_LIMIT_EXCEEDED;
        }
        if (raw.getExitCode() != 0) {
            return ExecutionVerdict.RUNTIME_ERROR;
        }
        String expected = testCase.getExpectedOutput();
        if (expected == null || expected.isBlank()) {
            return ExecutionVerdict.ACCEPTED;
        }
        String actual = raw.getStdout() == null ? "" : raw.getStdout().stripTrailing();
        return actual.equals(expected.stripTrailing()) ? ExecutionVerdict.ACCEPTED : ExecutionVerdict.WRONG_ANSWER;
    }

    private ExecResponse compileError(RawProcessResponse compile) {
        ExecResponse response = new ExecResponse();
        response.setVerdict(ExecutionVerdict.COMPILE_ERROR);
        response.setCompilerLogs(compile.getStderr());
        response.setPassedTests(0);
        response.setDurationMs(0);
        return response;
    }

    /** Look up how this executor runs the requested language; reject unsupported ones. */
    private LanguageConfig resolveConfig(Language language) {
        LanguageConfig config = CONFIG_MAP.get(language);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return config;
    }

    /** Compile if the language needs it; null = interpreted (nothing to compile). */
    private RawProcessResponse compileIfNeeded(Path workDir, LanguageConfig config)
            throws IOException, InterruptedException {
        if (config.getCompileCommand() == null) {
            return null;
        }
        return runCommand(workDir, config.getCompileCommand(), null, COMPILE_TIMEOUT);
    }

    private boolean compiledOk(RawProcessResponse compile) {
        return !compile.isTimedOut() && compile.getExitCode() == 0;
    }

    private void writeSource(Path workDir, LanguageConfig config, String code) throws IOException {
        Files.writeString(workDir.resolve(config.getFileName()), code == null ? "" : code);
    }

    private void writeStdin(Path workDir, String stdin) throws IOException {
        Files.writeString(workDir.resolve(STDIN_FILE), stdin == null ? "" : stdin);
    }

    /** Run the program (run command, stdin from the input file). */
    private RawProcessResponse runProcess(Path workDir, LanguageConfig config, Duration timeout)
            throws IOException, InterruptedException {
        return runCommand(workDir, config.getRunCommand(), workDir.resolve(STDIN_FILE), timeout);
    }

    /** Start a process with stdout/stderr captured to files (no pipe deadlock), enforce the timeout,
     *  kill the whole tree on overrun, and read the captured output back into a raw response. */
    private RawProcessResponse runCommand(Path workDir, java.util.List<String> command, Path stdin, Duration timeout)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir.toFile());
        if (stdin != null) {
            processBuilder.redirectInput(stdin.toFile());
        }
        processBuilder.redirectOutput(workDir.resolve(STDOUT_FILE).toFile());
        processBuilder.redirectError(workDir.resolve(STDERR_FILE).toFile());

        long startNanos = System.nanoTime();
        Process process = processBuilder.start();
        boolean finished;
        if (timeout != null) {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            process.waitFor();
            finished = true;
        }
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        boolean timedOut = !finished;
        int exitCode = -1;
        if (finished) {
            exitCode = process.exitValue();
        } else {
            killProcessTree(process);
        }
        return buildRaw(workDir, exitCode, timedOut, durationMs);
    }

    /** Kill the whole process tree (descendants too), not just the parent. */
    private void killProcessTree(Process process) throws InterruptedException {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        process.waitFor(); // wait for the kill to actually take effect
    }

    /** Read the captured stdout/stderr files into a raw response. */
    private RawProcessResponse buildRaw(Path workDir, int exitCode, boolean timedOut, long durationMs)
            throws IOException {
        RawProcessResponse response = new RawProcessResponse();
        response.setStdout(readCapped(workDir.resolve(STDOUT_FILE)));
        response.setStderr(readCapped(workDir.resolve(STDERR_FILE)));
        response.setExitCode(exitCode);
        response.setTimedOut(timedOut);
        response.setDurationMs(durationMs);
        return response;
    }

    /** Read at most MAX_OUTPUT_BYTES from a captured stream, flagging truncation — never loads a
     *  giant file fully into memory. */
    private String readCapped(Path file) throws IOException {
        long size = Files.size(file);
        try (InputStream in = Files.newInputStream(file)) {
            String text = new String(in.readNBytes(MAX_OUTPUT_BYTES), StandardCharsets.UTF_8);
            if (size > MAX_OUTPUT_BYTES) {
                return text + "\n… output truncated (" + size + " bytes total)";
            }
            return text;
        }
    }

    /** Recursively delete the temp work dir; cleanup failure must not mask the real result. */
    private void cleanup(Path workDir) {
        if (workDir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(workDir)) {
            paths.sorted(Comparator.reverseOrder()) // delete children before their parents
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            // swallow (we'll add logging later)
        }
    }
}
