package contract;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

/**
 * Test configuration for contract tests.
 *
 * <p>This configuration provides mock beans for external dependencies used in contract tests.
 * Contract tests should test the controller and service integration while mocking external
 * dependencies like HTTP clients and repositories.
 */
@TestConfiguration
@Profile("test")
public class ContractTestConfig {

  /**
   * Provides a mock ClientRegistrationRepository for OAuth client registrations.
   *
   * @return Mock ClientRegistrationRepository
   */
  @Bean
  @Primary
  public ClientRegistrationRepository clientRegistrationRepository() {
    return Mockito.mock(ClientRegistrationRepository.class);
  }

  /**
   * Provides a mock RestClient for external HTTP calls.
   *
   * @return Mock RestClient
   */
  @Bean
  @Primary
  public RestClient restClient() {
    return Mockito.mock(RestClient.class);
  }
}
