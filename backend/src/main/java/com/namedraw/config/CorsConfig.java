package com.namedraw.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration.
 *
 * <p>Allows the frontend application to call the backend API. Allowed origins are configurable via
 * the property: app.cors.allowed-origins (comma-separated). Defaults to http://localhost:5173 for
 * development if not specified.
 */
@Configuration
public class CorsConfig {

  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private String allowedOriginsProp;

  /**
   * Define the CORS configuration source bean used by Spring Security.
   *
   * <p>It applies to API endpoints under /api/** and the H2 console in development. Allowed origins
   * are read from the application property {@code app.cors.allowed-origins} as a comma-separated
   * list.
   *
   * @return a CorsConfigurationSource configured with allowed origins, headers, and methods
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    List<String> allowedOrigins =
        Arrays.stream(allowedOriginsProp.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    configuration.setAllowedOrigins(allowedOrigins);

    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setExposedHeaders(Arrays.asList("Authorization"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    source.registerCorsConfiguration("/h2-console/**", configuration);
    return source;
  }
}
