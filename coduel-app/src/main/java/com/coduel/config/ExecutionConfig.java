package com.coduel.config;

import com.coduel.execution.impl.ProcessBuilderExecutor;
import com.coduel.execution.interfaces.CodeExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutionConfig {

    // The execution engine lives in the plain-Java coduel-execution module (no Spring annotations),
    // so it's wired as a bean here. Used by both workers — the judge (JudgeDto) and runs (RunDto).
    // The old synchronous bounded thread pool is gone: execution is now bounded by the RabbitMQ
    // consumer concurrency, not an in-process executor.
    @Bean
    public CodeExecutor codeExecutor() {
        return new ProcessBuilderExecutor();
    }
}
