package contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
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
 * Contract test for GET /auth/login/{provider}
 *
 * <p>This test validates the OAuth login initiation endpoint according to the OpenAPI
 * specification. The endpoint should redirect users to the OAuth provider for authentication.
 *
 * <p>OpenAPI Contract: - Path: GET /auth/login/{provider} - Parameters: provider (path) - enum:
 * [facebook, google] - Responses: - 302: Redirect to OAuth provider - 400: Invalid provider
 * (ErrorResponse schema)
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Contract Test: GET /auth/login/{provider}")
class AuthLoginContractTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired private RestClient restClient;

  @BeforeEach
  void setUp() {
    Mockito.reset(clientRegistrationRepository, restClient);
    setupGoogleClientRegistration();
    setupFacebookClientRegistration();
  }

  private void setupGoogleClientRegistration() {
    ClientRegistration googleRegistration =
        ClientRegistration.withRegistrationId("google")
            .clientId("test-google-client-id")
            .clientSecret("test-google-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/api/v1/auth/callback/{registrationId}")
            .scope("profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
            .clientName("Google")
            .build();

    when(clientRegistrationRepository.findByRegistrationId("google"))
        .thenReturn(googleRegistration);
  }

  private void setupFacebookClientRegistration() {
    ClientRegistration facebookRegistration =
        ClientRegistration.withRegistrationId("facebook")
            .clientId("test-facebook-client-id")
            .clientSecret("test-facebook-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/api/v1/auth/callback/{registrationId}")
            .scope("email", "public_profile")
            .authorizationUri("https://www.facebook.com/v18.0/dialog/oauth")
            .tokenUri("https://graph.facebook.com/v18.0/oauth/access_token")
            .userInfoUri("https://graph.facebook.com/v18.0/me?fields=id,name,email")
            .clientName("Facebook")
            .build();

    when(clientRegistrationRepository.findByRegistrationId("facebook"))
        .thenReturn(facebookRegistration);
  }

  @Test
  @DisplayName("Should redirect to Google OAuth provider when provider is 'google'")
  void shouldRedirectToGoogleOAuthProvider() throws Exception {
    // Given a request to login with Google
    String provider = "google";

    // When calling the OAuth login endpoint
    sendLoginRequest(provider)

        // Then should redirect to Google OAuth provider
        .andExpect(status().isFound()) // 302 redirect
        .andExpect(header().exists("Location"))
        .andExpect(
            header()
                .string("Location", org.hamcrest.Matchers.containsString("accounts.google.com")));
  }

  @Test
  @DisplayName("Should redirect to Facebook OAuth provider when provider is 'facebook'")
  void shouldRedirectToFacebookOAuthProvider() throws Exception {
    // Given a request to login with Facebook
    String provider = "facebook";

    // When calling the OAuth login endpoint
    sendLoginRequest(provider)

        // Then should redirect to Facebook OAuth provider
        .andExpect(status().isFound()) // 302 redirect
        .andExpect(header().exists("Location"))
        .andExpect(
            header().string("Location", org.hamcrest.Matchers.containsString("facebook.com")));
  }

  @Test
  @DisplayName("Should return 400 Bad Request for invalid provider")
  void shouldReturnBadRequestForInvalidProvider() throws Exception {
    // Given a request with invalid provider
    String invalidProvider = "twitter";

    // When calling the OAuth login endpoint
    sendLoginRequest(invalidProvider)

        // Then should return 400 Bad Request with ErrorResponse schema
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/login/" + invalidProvider));
  }

  @Test
  @DisplayName("Should include CSRF protection in OAuth redirect")
  void shouldIncludeCSRFProtectionInRedirect() throws Exception {
    // Given a request to login with Google
    String provider = "google";

    // When calling the OAuth login endpoint
    sendLoginRequest(provider)

        // Then should include state parameter for CSRF protection
        .andExpect(status().isFound())
        .andExpect(header().exists("Location"))
        .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("state=")));
  }

  @Test
  @DisplayName("Should include required OAuth scopes in redirect")
  void shouldIncludeRequiredOAuthScopesInRedirect() throws Exception {
    // Given a request to login with Google
    String provider = "google";

    // When calling the OAuth login endpoint
    sendLoginRequest(provider)

        // Then should include required scopes (profile, email)
        .andExpect(status().isFound())
        .andExpect(header().exists("Location"))
        .andExpect(
            header()
                .string(
                    "Location",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("scope="),
                        org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.containsString("profile"),
                            org.hamcrest.Matchers.containsString("email")))));
  }

  private ResultActions sendLoginRequest(String invalidProvider) throws Exception {
    return mockMvc.perform(
        get("/api/v1/auth/login/{provider}", invalidProvider)
            .contentType(MediaType.APPLICATION_JSON));
  }
}
