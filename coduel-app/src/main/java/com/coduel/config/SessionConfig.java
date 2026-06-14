package com.coduel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionConfig {

    // Without a cookie Max-Age the browser treats SESSION as a session cookie and drops it on
    // browser close / sleep-resume — so the user appears logged out even though Redis still holds
    // the session. Derive Max-Age from the same timeout Spring uses for the Redis TTL.
    @Bean
    public CookieSerializer cookieSerializer(AppProperties appProperties) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseHttpOnlyCookie(true);
        // dev: same-site (localhost:5173 <-> :8080). Prod cross-site domains need "None" + Secure.
        serializer.setSameSite("Lax");
        serializer.setCookieMaxAge((int) appProperties.getSessionTimeout().toSeconds());
        return serializer;
    }
}
