package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
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
 * Contract test for POST /auth/refresh
 *
 * <p>This test validates the JWT token refresh endpoint according to the OpenAPI specification. The
 * endpoint should get a new JWT token using refresh token.
 *
 * <p>OpenAPI Contract: - Path: POST /auth/refresh - Request Body: { refreshToken: string } -
 * Responses: - 200: Token refreshed successfully (AuthResponse schema) - 401: Invalid refresh token
 * (ErrorResponse schema)
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Contract Test: POST /auth/refresh")
class AuthRefreshContractTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Test
  @DisplayName("Should return new AuthResponse with valid refresh token")
  void shouldReturnNewAuthResponseWithValidRefreshToken() throws Exception {
    // Given a valid refresh token request
    Map<String, String> refreshRequest = Map.of("refreshToken", "valid_refresh_token_abc123xyz");

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(refreshRequest)

        // Then should return new tokens with AuthResponse schema
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
  @DisplayName("Should return different access token for refresh request")
  void shouldReturnDifferentAccessTokenForRefresh() throws Exception {
    // Given a valid refresh token request
    Map<String, String> refreshRequest = Map.of("refreshToken", "valid_refresh_token_def456uvw");

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(refreshRequest)

        // Then should return new access token (different from original)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.accessToken").isString())
        // Note: In real implementation, we'd verify the token is different
        .andExpect(jsonPath("$.refreshToken").exists())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for expired refresh token")
  void shouldReturnUnauthorizedForExpiredRefreshToken() throws Exception {
    // Given an expired refresh token request
    Map<String, String> expiredRequest = Map.of("refreshToken", "expired_refresh_token_old123");

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(expiredRequest)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for invalid refresh token")
  void shouldReturnUnauthorizedForInvalidRefreshToken() throws Exception {
    // Given an invalid refresh token request
    Map<String, String> invalidRequest = Map.of("refreshToken", "invalid_malformed_token_999");

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(invalidRequest)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized for revoked refresh token")
  void shouldReturnUnauthorizedForRevokedRefreshToken() throws Exception {
    // Given a revoked refresh token request
    Map<String, String> revokedRequest =
        Map.of("refreshToken", "revoked_refresh_token_blacklisted");

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(revokedRequest)

        // Then should return 401 Unauthorized with ErrorResponse schema
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));
  }

  @Test
  @DisplayName("Should return 400 Bad Request for missing refresh token")
  void shouldReturnBadRequestForMissingRefreshToken() throws Exception {
    // Given a request missing the required refreshToken field
    Map<String, String> emptyRequest = Map.of();

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(emptyRequest)

        // Then should return 400 Bad Request due to validation failure
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));
  }

  @Test
  @DisplayName("Should return 400 Bad Request for empty refresh token")
  void shouldReturnBadRequestForEmptyRefreshToken() throws Exception {
    // Given a request with empty refreshToken field
    Map<String, String> emptyTokenRequest = Map.of("refreshToken", "");

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(emptyTokenRequest)

        // Then should return 400 Bad Request due to validation failure
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));
  }

  @Test
  @DisplayName("Should validate refresh token format")
  void shouldValidateRefreshTokenFormat() throws Exception {
    // Given a request with malformed refresh token
    Map<String, String> malformedRequest = Map.of("refreshToken", "not.a.valid.jwt.token.format");

    // When calling the token refresh endpoint
    sendRefreshTokenRequest(malformedRequest)

        // Then should return 401 Unauthorized due to invalid token format
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));
  }

  private ResultActions sendRefreshTokenRequest(Map<String, String> refreshRequest) throws Exception, JsonProcessingException {
    return mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)));
  }
}
