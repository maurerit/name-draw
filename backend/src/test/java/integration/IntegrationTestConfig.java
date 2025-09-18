package integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@TestConfiguration
@Profile("test")
public class IntegrationTestConfig {

  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder.build();
  }
}
