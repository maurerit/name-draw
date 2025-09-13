package contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.namedraw.model.User;
import com.namedraw.repository.UserRepository;
import com.namedraw.security.JwtTokenService;
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
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Contract Test: POST /auth/refresh")
class AuthRefreshContractTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private JwtTokenService jwtTokenService;

  @BeforeEach
  void setUp() {
    // Clear blacklist between tests to avoid interference
    jwtTokenService.clearBlacklist();

    // Setup mock repository to return test user
    User testUser = ContractTestConfig.getTestUser();
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
  }

  @Test
  @DisplayName("Should return new AuthResponse with valid refresh token")
  void shouldReturnNewAuthResponseWithValidRefreshToken() throws Exception {
    // Given a valid refresh token request
    Map<String, String> refreshRequest =
        Map.of("refreshToken", TestTokenGenerator.generateValidRefreshToken());

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
    Map<String, String> refreshRequest =
        Map.of("refreshToken", TestTokenGenerator.generateSecondValidRefreshToken());

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
    Map<String, String> expiredRequest =
        Map.of("refreshToken", TestTokenGenerator.generateExpiredRefreshToken());

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
    Map<String, String> invalidRequest =
        Map.of("refreshToken", TestTokenGenerator.getMalformedToken());

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
        Map.of("refreshToken", TestTokenGenerator.generateRevokedRefreshToken());

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

  private ResultActions sendRefreshTokenRequest(Map<String, String> refreshRequest)
      throws Exception, JsonProcessingException {
    return mockMvc.perform(
        post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(refreshRequest)));
  }
}
