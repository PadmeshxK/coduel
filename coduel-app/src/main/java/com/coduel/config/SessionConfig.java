package com.coduel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

// Spring Boot 4's session auto-config wasn't engaging (the cookie was Tomcat's in-memory JSESSIONID,
// nothing was written to Redis). Enable Spring Session explicitly: the non-indexed
// RedisSessionRepository (no CONFIG / keyspace-notification commands → works on managed Redis like
// Upstash), with a 30-day rolling TTL that matches the SESSION cookie Max-Age below.
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 60 * 60 * 24 * 30)
public class SessionConfig {

    // Without a cookie Max-Age the browser treats SESSION as a session cookie and drops it on
    // browser close / sleep-resume — so the user appears logged out even though Redis still holds
    // the session. Derive Max-Age from the same timeout Spring uses for the Redis TTL.
    @Bean
    public CookieSerializer cookieSerializer(AppProperties appProperties) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseHttpOnlyCookie(true);
        // The SPA and API live on different sites in prod, so the SESSION cookie is always
        // SameSite=None + Secure — the only combination the browser sends on cross-site fetches.
        // (http://localhost is a secure context, so Secure cookies work in local dev too.)
        serializer.setSameSite("None");
        serializer.setUseSecureCookie(true);
        serializer.setCookieMaxAge((int) appProperties.getSessionTimeout().toSeconds());
        return serializer;
    }
}
