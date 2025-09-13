package com.namedraw.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Name Draw application.
 *
 * <p>This configuration sets up security rules for different endpoints: - OAuth authentication
 * endpoints are publicly accessible - H2 console is accessible in development/test profiles - All
 * other endpoints require authentication
 *
 * <p>The application uses OAuth2 for authentication with Google and Facebook providers, and JWT
 * tokens for session management.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Configure HTTP security filter chain.
   *
   * @param http HttpSecurity configuration
   * @return SecurityFilterChain for the application
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz ->
                authz
                    // Allow all OAuth authentication endpoints
                    .requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    // Allow H2 console for development/testing
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    // All other endpoints require authentication
                    .anyRequest()
                    .authenticated())
        .sessionManagement(
            session ->
                session.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
        .csrf(
            csrf ->
                csrf
                    // Disable CSRF for H2 console and API endpoints
                    .ignoringRequestMatchers("/h2-console/**", "/api/v1/**"))
        .headers(
            headers ->
                headers
                    // Allow frames for H2 console
                    .frameOptions(frame -> frame.sameOrigin()));

    return http.build();
  }
}
