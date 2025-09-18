package contract;

import com.namedraw.client.AuthClient;
import com.namedraw.model.User;
import com.namedraw.repository.DrawRepository;
import com.namedraw.repository.UserRepository;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@TestConfiguration
@Profile("test")
public class ContractTestConfig {

  private static User testUser;

  // Mock repositories and external clients instead of services for integration tests
  @MockBean private UserRepository userRepository;
  @MockBean private DrawRepository drawRepository;

  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private AuthClient authClient;

  static {
    testUser =
        User.builder()
            .id(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
            .oauthProvider("google")
            .oauthId("test123")
            .name("Test User")
            .email("test@example.com")
            .isActive(true)
            .build();
  }

  public static User getTestUser() {
    return testUser;
  }

  // No additional beans; mocks above will replace real beans in the context
}
