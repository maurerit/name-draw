package contract;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.namedraw.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClient;

/**
 * Contract test for POST /auth/callback/{provider}
 *
 * <p>This test validates the OAuth callback handling endpoint according to the OpenAPI
 * specification. The endpoint should process OAuth callback and return JWT token.
 *
 * <p>OpenAPI Contract: - Path: POST /auth/callback/{provider} - Parameters: provider (path) - enum:
 * [facebook, google] - Request Body: { code: string, state: string } - Responses: - 200: Successful
 * authentication (AuthResponse schema) - 400: Invalid request (ErrorResponse schema) - 401:
 * Authentication failed (ErrorResponse schema)
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Contract Test: POST /auth/callback/{provider}")
class AuthCallbackContractTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired private RestClient restClient;

  @BeforeEach
  void setUp() {
    // Reset all mocks
    reset(userRepository, clientRegistrationRepository, restClient);

    // Mock UserRepository to return test user
    when(userRepository.findByOauthProviderAndOauthId(anyString(), anyString()))
        .thenReturn(Optional.of(ContractTestConfig.getTestUser()));
    when(userRepository.save(any())).thenReturn(ContractTestConfig.getTestUser());

    // Mock ClientRegistrationRepository for google
    ClientRegistration googleRegistration =
        ClientRegistration.withRegistrationId("google")
            .clientId("test-google-client-id")
            .clientSecret("test-google-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/api/v1/auth/callback/google")
            .authorizationUri("https://accounts.google.com/o/oauth2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
            .userNameAttributeName("email")
            .clientName("Google")
            .build();
    when(clientRegistrationRepository.findByRegistrationId("google"))
        .thenReturn(googleRegistration);

    // Mock ClientRegistrationRepository for facebook
    ClientRegistration facebookRegistration =
        ClientRegistration.withRegistrationId("facebook")
            .clientId("test-facebook-client-id")
            .clientSecret("test-facebook-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/api/v1/auth/callback/facebook")
            .authorizationUri("https://www.facebook.com/v12.0/dialog/oauth")
            .tokenUri("https://graph.facebook.com/v12.0/oauth/access_token")
            .userInfoUri("https://graph.facebook.com/me?fields=id,name,email")
            .userNameAttributeName("email")
            .clientName("Facebook")
            .build();
    when(clientRegistrationRepository.findByRegistrationId("facebook"))
        .thenReturn(facebookRegistration);

    // Mock invalid providers
    when(clientRegistrationRepository.findByRegistrationId("twitter")).thenReturn(null);
  }

  @Test
  @DisplayName("Should return AuthResponse with valid Google OAuth callback")
  void shouldReturnAuthResponseWithValidGoogleCallback() throws Exception {
    // Given a valid OAuth callback request for Google
    String provider = "google";
    Map<String, String> callbackRequest =
        Map.of(
            "code", "valid_google_auth_code_12345",
            "state", "csrf_protection_state_token");

    // When calling the OAuth callback endpoint
    sendOAuthCallback(provider, callbackRequest)

        // Then should return successful authentication with AuthResponse schema
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").isNumber())
        .andExpect(jsonPath("$.user").exists())
        .andExpect(jsonPath("$.user.id").exists())
        .andExpect(jsonPath("$.user.name").exists())
        .andExpect(jsonPath("$.user.isActive").value(true));
  }

  @Test
  @DisplayName("Should return AuthResponse with valid Facebook OAuth callback")
  void shouldReturnAuthResponseWithValidFacebookCallback() throws Exception {
    // Given a valid OAuth callback request for Facebook
    String provider = "facebook";
    Map<String, String> callbackRequest =
        Map.of(
            "code", "valid_facebook_auth_code_67890",
            "state", "csrf_protection_state_token");

    // When calling the OAuth callback endpoint
    sendOAuthCallback(provider, callbackRequest)

        // Then should return successful authentication with AuthResponse schema
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.refreshToken").exists())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").isNumber())
        .andExpect(jsonPath("$.user").exists())
        .andExpect(jsonPath("$.user.id").exists())
        .andExpect(jsonPath("$.user.name").exists())
        .andExpect(jsonPath("$.user.isActive").value(true));
  }

  @Test
  @DisplayName("Should return 400 Bad Request for missing code parameter")
  void shouldReturnBadRequestForMissingCode() throws Exception {
    // Given a callback request missing the required code parameter
    String provider = "google";
    Map<String, String> invalidRequest =
        Map.of(
            "state", "csrf_protection_state_token"
            // Missing required 'code' parameter
            );

    // When calling the OAuth callback endpoint
    sendOAuthCallback(provider, invalidRequest)

        // Then should return 400 Bad Request with ErrorResponse schema
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/callback/" + provider));
  }

  @Test
  @DisplayName("Should return 400 Bad Request for missing state parameter")
  void shouldReturnBadRequestForMissingState() throws Exception {
    // Given a callback request missing the required state parameter
    String provider = "facebook";
    Map<String, String> invalidRequest =
        Map.of(
            "code", "valid_facebook_auth_code_67890"
            // Missing required 'state' parameter
            );

    // When calling the OAuth callback endpoint
    sendOAuthCallback(provider, invalidRequest)

        // Then should return 400 Bad Request with ErrorResponse schema
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/callback/" + provider));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for invalid authorization code")
  void shouldReturnUnauthorizedForInvalidCode() throws Exception {
    // Given a callback request with invalid authorization code
    String provider = "google";
    Map<String, String> invalidRequest =
        Map.of(
            "code", "invalid_or_expired_auth_code",
            "state", "csrf_protection_state_token");

    // When calling the OAuth callback endpoint
    sendOAuthCallback(provider, invalidRequest)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/callback/" + provider));
  }

  @Test
  @DisplayName("Should return 400 Bad Request for invalid provider")
  void shouldReturnBadRequestForInvalidProvider() throws Exception {
    // Given a callback request with invalid provider
    String invalidProvider = "twitter";
    Map<String, String> callbackRequest =
        Map.of(
            "code", "valid_auth_code_12345",
            "state", "csrf_protection_state_token");

    // When calling the OAuth callback endpoint
    sendOAuthCallback(invalidProvider, callbackRequest)

        // Then should return 400 Bad Request with ErrorResponse schema
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/callback/" + invalidProvider));
  }

  @Test
  @DisplayName("Should validate CSRF state parameter")
  void shouldValidateCSRFStateParameter() throws Exception {
    // Given a callback request with mismatched state parameter
    String provider = "google";
    Map<String, String> callbackRequest =
        Map.of(
            "code", "valid_google_auth_code_12345",
            "state", "mismatched_state_token");

    // When calling the OAuth callback endpoint
    sendOAuthCallback(provider, callbackRequest)

        // Then should return 401 Unauthorized due to CSRF validation failure
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/callback/" + provider));
  }

  private ResultActions sendOAuthCallback(String provider, Map<String, String> callbackRequest)
      throws Exception, JsonProcessingException {
    return mockMvc.perform(
        post("/api/v1/auth/callback/{provider}", provider)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(callbackRequest)));
  }
}
