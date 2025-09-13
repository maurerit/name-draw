package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Contract Test: GET /auth/login/{provider}")
class AuthLoginContractTest {

  @Autowired private MockMvc mockMvc;

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
