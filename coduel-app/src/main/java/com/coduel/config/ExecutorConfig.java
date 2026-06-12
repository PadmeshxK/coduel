package com.coduel.config;

import com.coduel.execution.impl.ProcessBuilderExecutor;
import com.coduel.execution.interfaces.CodeExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExecutorConfig {

    @Bean
    public CodeExecutor codeExecutor() {
        return new ProcessBuilderExecutor();
    }

    /** Fixed pool + bounded queue; a full queue rejects (AbortPolicy), which the Dto maps to 503. */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService executionExecutor(AppProperties properties) {
        return new ThreadPoolExecutor(
                properties.getPoolSize(),
                properties.getPoolSize(),
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()));
    }
}
