package com.namedraw.security;

import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
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
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${jwt.secret}")
  private String jwtSecret;

  private final JwtTokenService jwtTokenService;

  /**
   * Configure JWT decoder for validating access tokens.
   *
   * @return JwtDecoder configured with the application's JWT secret and blacklist checking
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    NimbusJwtDecoder nimbusDecoder =
        NimbusJwtDecoder.withSecretKey(Keys.hmacShaKeyFor(secretBytes))
            .macAlgorithm(MacAlgorithm.HS384)
            .build();

    // Wrap the decoder to add blacklist checking
    return new BlacklistAwareJwtDecoder(nimbusDecoder, jwtTokenService);
  }

  /**
   * Custom authentication entry point that returns JSON error responses.
   *
   * @return AuthenticationEntryPoint for JWT authentication failures
   */
  @Bean
  public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
    return (HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);

      String errorResponse =
          String.format(
              "{\"error\":\"Unauthorized\",\"message\":\"%s\","
                  + "\"timestamp\":\"%s\",\"path\":\"%s\"}",
              "Invalid or missing JWT token", Instant.now().toString(), request.getRequestURI());

      response.getWriter().write(errorResponse);
    };
  }

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
                    // Allow OAuth authentication endpoints except logout which requires auth
                    .requestMatchers(
                        "/api/v1/auth/login/**", "/api/v1/auth/callback/**", "/api/v1/auth/refresh")
                    .permitAll()
                    // Logout requires authentication
                    .requestMatchers("/api/v1/auth/logout")
                    .authenticated()
                    // Allow H2 console for development/testing
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    // All other endpoints require authentication
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.decoder(jwtDecoder()))
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint()))
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
