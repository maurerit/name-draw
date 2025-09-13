package com.namedraw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

/**
 * Configuration for HTTP client beans.
 *
 * <p>This configuration provides RestClient beans for making HTTP requests to external services
 * such as OAuth providers. RestClient is the modern replacement for RestTemplate and offers a more
 * fluent API for HTTP operations.
 */
@Configuration
@Profile("!test") // Exclude from test profile
public class HttpClientConfig {

  /**
   * Creates a RestClient bean for making HTTP requests.
   *
   * @return Configured RestClient instance
   */
  @Bean
  public RestClient restClient() {
    return RestClient.builder().build();
  }
}
