package com.coduel.security;

import com.coduel.config.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CoduelOidcUserService oidcUserService,
                                                   AppProperties appProperties) throws Exception {
        http
                // Enable CORS using the CorsConfigurationSource bean below (preflight + cross-origin SPA).
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/error").permitAll()
                        // Uploaded media is served by unguessable UUID URL (capability), so the image GET
                        // needs no auth — and <img> tags can't carry the session cookie cross-origin anyway.
                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated()
                )
                // SPA-friendly: unauthenticated API calls get 401 (the frontend routes to /login),
                // not the default 302 redirect to Google that an XHR can't follow.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(info -> info.oidcUserService(oidcUserService))
                        // After Google login, bounce to the SPA (configurable per env), not the backend root.
                        .defaultSuccessUrl(appProperties.getFrontendBaseUrl(), true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")                 // POST /coduel/logout
                        .invalidateHttpSession(true)          // kill the session in Redis (instant revocation)
                        .deleteCookies("SESSION")             // clear the spring-session cookie
                        // API-style: 200 instead of a redirect (the SPA handles navigation)
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                )
                // TODO: re-enable with CookieCsrfTokenRepository once the React SPA lands.
                // Disabled now so the API stays curl-testable while there's no browser frontend.
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration config = new CorsConfiguration();
        // Single source of truth: the SPA origin (frontend.base-url). Explicit origin (NOT "*")
        // is required because we send the SESSION cookie cross-origin.
        config.setAllowedOrigins(List.of(appProperties.getFrontendBaseUrl()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);   // let the browser attach/receive the SESSION cookie
        config.setMaxAge(3600L);            // cache the preflight response for 1h

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
