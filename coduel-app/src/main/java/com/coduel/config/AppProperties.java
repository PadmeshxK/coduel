package com.coduel.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Getter
@Component
public class AppProperties {

    // 0 = auto: size the pool to the number of available CPUs.
    @Value("${execution.pool-size:0}")
    private int poolSize;

    @Value("${execution.queue-capacity:50}")
    private int queueCapacity;

    @Value("${execution.default-timeout-ms:5000}")
    private long defaultTimeoutMs;

    @Value("${execution.max-timeout-ms:10000}")
    private long maxTimeoutMs;

    @PostConstruct
    public void init() {
        if (poolSize <= 0) {
            poolSize = Runtime.getRuntime().availableProcessors();
        }
        trimStringProperties();
    }

    /** Trim every String property in place (dormant until a String property exists). */
    private void trimStringProperties() {
        for (Field field : getClass().getDeclaredFields()) {
            if (!String.class.equals(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            try {
                String value = (String) field.get(this);
                if (value != null) {
                    field.set(this, value.trim());
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
