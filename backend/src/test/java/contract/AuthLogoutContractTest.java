package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contract test for POST /auth/logout
 *
 * <p>This test validates the user logout endpoint according to the OpenAPI specification. The
 * endpoint should invalidate JWT token and logout user.
 *
 * <p>OpenAPI Contract: - Path: POST /auth/logout - Security: BearerAuth (JWT token required) -
 * Responses: - 200: Successfully logged out - 401: Unauthorized (ErrorResponse schema)
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Contract Test: POST /auth/logout")
class AuthLogoutContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("Should return 200 OK for successful logout with valid JWT token")
  void shouldReturnOkForSuccessfulLogoutWithValidToken() throws Exception {
    // Given a valid JWT token for authenticated user
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.validTokenPayload.signature";

    // When calling the logout endpoint with valid authorization
    sendLogoutRequest(validJwtToken)

        // Then should return 200 OK for successful logout
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for logout without authorization header")
  void shouldReturnUnauthorizedForLogoutWithoutAuthHeader() throws Exception {
    // Given a logout request without authorization header

    // When calling the logout endpoint without authorization
    mockMvc
        .perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON))

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for logout with invalid JWT token")
  void shouldReturnUnauthorizedForLogoutWithInvalidToken() throws Exception {
    // Given an invalid JWT token
    String invalidJwtToken = "Bearer invalid.jwt.token.format";

    // When calling the logout endpoint with invalid authorization
    sendLogoutRequest(invalidJwtToken)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for logout with expired JWT token")
  void shouldReturnUnauthorizedForLogoutWithExpiredToken() throws Exception {
    // Given an expired JWT token
    String expiredJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expiredTokenPayload.signature";

    // When calling the logout endpoint with expired authorization
    sendLogoutRequest(expiredJwtToken)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for logout with malformed authorization header")
  void shouldReturnUnauthorizedForLogoutWithMalformedAuthHeader() throws Exception {
    // Given a malformed authorization header (missing Bearer prefix)
    String malformedAuthHeader =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tokenWithoutBearer.signature";

    // When calling the logout endpoint with malformed authorization
    sendLogoutRequest(malformedAuthHeader)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for logout with revoked JWT token")
  void shouldReturnUnauthorizedForLogoutWithRevokedToken() throws Exception {
    // Given a revoked JWT token (blacklisted)
    String revokedJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.revokedTokenPayload.signature";

    // When calling the logout endpoint with revoked authorization
    sendLogoutRequest(revokedJwtToken)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));
  }

  @Test
  @DisplayName("Should invalidate JWT token after successful logout")
  void shouldInvalidateJwtTokenAfterSuccessfulLogout() throws Exception {
    // Given a valid JWT token for authenticated user
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.validTokenPayload.signature";

    // When calling the logout endpoint with valid authorization
    sendLogoutRequest(validJwtToken)

        // Then should return 200 OK for successful logout
        .andExpect(status().isOk());

    // And when attempting to use the same token again (e.g., for another logout)
    sendLogoutRequest(validJwtToken)

        // Then should return 401 Unauthorized (token has been invalidated)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));
  }

  @Test
  @DisplayName("Should handle logout with empty Bearer token")
  void shouldHandleLogoutWithEmptyBearerToken() throws Exception {
    // Given an authorization header with empty Bearer token
    String emptyBearerToken = "Bearer ";

    // When calling the logout endpoint with empty Bearer token
    sendLogoutRequest(emptyBearerToken)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/logout"));
  }

  private ResultActions sendLogoutRequest(String validJwtToken) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/logout")
            .header(HttpHeaders.AUTHORIZATION, validJwtToken)
            .contentType(MediaType.APPLICATION_JSON));
  }
}
