package com.coduel.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Duration;

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

    // Where the browser SPA is served. Single source of truth used for BOTH the OAuth2 success
    // redirect and the CORS allowed origin (one frontend = one browser origin allowed to call us).
    @Value("${frontend.base-url}")
    private String frontendBaseUrl;

    // Session lifetime — also used as the SESSION cookie Max-Age. Mirrors spring.session.timeout
    // (which Spring uses for the Redis TTL) so the cookie and server-side session share one value.
    @Value("${spring.session.timeout}")
    private Duration sessionTimeout;

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
