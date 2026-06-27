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

    // Directory that holds uploaded media (chat images). Dev default = under the user's home; prod can
    // point elsewhere (or a future adapter can swap to S3). Files are served at /uploads/**.
    @Value("${uploads.dir:${user.home}/.coduel/uploads}")
    private String uploadsDir;

    // Session lifetime — also used as the SESSION cookie Max-Age. Mirrors spring.session.timeout
    // (which Spring uses for the Redis TTL) so the cookie and server-side session share one value.
    @Value("${spring.session.timeout}")
    private Duration sessionTimeout;

    // SESSION cookie SameSite policy. Subdomain setup (SPA + API both under one site, e.g. coduel.org
    // + api.coduel.org) is same-site → Lax. A cross-SITE split (different registrable domains) needs
    // None. Default Lax; override per env.
    @Value("${session.cookie.same-site:Lax}")
    private String cookieSameSite;

    // ---- Media storage (Cloudflare R2). Secrets come from the env; the rest are non-secret. ----
    @Value("${media.storage:local}")
    private String mediaStorage;

    @Value("${r2.endpoint:}")
    private String r2Endpoint;

    @Value("${r2.bucket:}")
    private String r2Bucket;

    @Value("${r2.access-key:}")
    private String r2AccessKey;

    @Value("${r2.secret-key:}")
    private String r2SecretKey;

    // Public read URL (r2.dev or a custom domain) prepended to a stored object's key for the client.
    @Value("${r2.public-base-url:}")
    private String r2PublicBaseUrl;

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
