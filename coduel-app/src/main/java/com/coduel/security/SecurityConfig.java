package com.coduel.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CoduelOidcUserService oidcUserService) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/error").permitAll()
                        // public catalog: browsing problems doesn't require login
                        .requestMatchers(HttpMethod.GET, "/problems", "/problems/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(info -> info.oidcUserService(oidcUserService))
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
}
