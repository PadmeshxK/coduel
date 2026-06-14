package com.coduel.execution.impl;

import com.coduel.execution.exception.CodeExecutionException;
import com.coduel.execution.helper.ConversionHelper;
import com.coduel.execution.interfaces.CodeExecutor;
import com.coduel.execution.model.config.LanguageConfig;
import com.coduel.execution.model.constant.Language;
import com.coduel.execution.model.request.ExecRequest;
import com.coduel.execution.model.response.ExecResponse;

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

    // Cap per stream — a runaway program (e.g. an infinite print loop) can't OOM us or balloon the
    // response. We read at most this many bytes back even if the captured file is far larger.
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    private static final Map<Language, LanguageConfig> CONFIG_MAP = ConversionHelper.constructConfig();

    public ExecResponse run(ExecRequest request) {
        LanguageConfig config = resolveConfig(request.getLanguage());

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("coduel-");
            writeInputFiles(workDir, config, request);
            Process process = startProcess(workDir, config);
            return waitAndBuildResponse(process, workDir, request.getTimeout());
        } catch (IOException e) {
            throw new CodeExecutionException("Failed to execute code", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the interrupt flag
            throw new CodeExecutionException("Execution interrupted", e);
        } finally {
            cleanup(workDir);
        }
    }

    /** Look up how this executor runs the requested language; reject unsupported ones. */
    private LanguageConfig resolveConfig(Language language) {
        LanguageConfig config = CONFIG_MAP.get(language);
        if (config == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return config;
    }

    /** Write the submitted code to its source file, and stdin to an input file. */
    private void writeInputFiles(Path workDir, LanguageConfig config, ExecRequest request) throws IOException {
        Files.writeString(workDir.resolve(config.getFileName()), request.getCode());
        String stdin = request.getStdin() == null ? "" : request.getStdin();
        Files.writeString(workDir.resolve(STDIN_FILE), stdin);
    }

    /** Build and start the process, with all three streams redirected to files (no pipe deadlock). */
    private Process startProcess(Path workDir, LanguageConfig config) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(config.getRunCommand());
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectInput(workDir.resolve(STDIN_FILE).toFile());
        processBuilder.redirectOutput(workDir.resolve(STDOUT_FILE).toFile());
        processBuilder.redirectError(workDir.resolve(STDERR_FILE).toFile());
        return processBuilder.start();
    }

    /** Wait (enforcing the timeout), kill on overrun, then read the captured output into a response. */
    private ExecResponse waitAndBuildResponse(Process process, Path workDir, Duration timeout)
            throws IOException, InterruptedException {
        long startNanos = System.nanoTime();
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
        return buildResponse(workDir, exitCode, timedOut, durationMs);
    }

    /** Kill the whole process tree (descendants too), not just the parent. */
    private void killProcessTree(Process process) throws InterruptedException {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
        process.waitFor(); // wait for the kill to actually take effect
    }

    /** Read the captured stdout/stderr files and assemble the response. */
    private ExecResponse buildResponse(Path workDir, int exitCode, boolean timedOut, long durationMs)
            throws IOException {
        ExecResponse response = new ExecResponse();
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
