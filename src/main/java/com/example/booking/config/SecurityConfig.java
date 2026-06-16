package com.example.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Segment 1 security setup.
 *
 * <p>Authentication is HTTP Basic against two in-memory demo users (see
 * {@link #userDetailsService}). This is deliberately minimal: the assignment
 * explicitly lists OAuth/SSO/MFA as out of scope, and Basic auth is trivial to
 * exercise from Swagger's "Authorize" dialog during review.
 *
 * <p>Authorization is role-based: anything under {@code /api/admin/**} requires
 * ROLE_ADMIN; every other endpoint requires an authenticated user. The Swagger
 * UI and the OpenAPI document are left public so the API can be browsed without
 * logging in.
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // The API is stateless and authenticated per-request via HTTP Basic.
                // CSRF protection guards cookie/session-based browser flows, which we
                // do not use, so it is disabled here.
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        UserDetails customer = User.withUsername("customer")
                .password(encoder.encode("customer123"))
                .roles("CUSTOMER")
                .build();
        return new InMemoryUserDetailsManager(admin, customer);
    }
}
