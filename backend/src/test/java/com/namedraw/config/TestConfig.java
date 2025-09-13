package com.namedraw.config;

import com.namedraw.model.User;
import com.namedraw.security.JwtTokenService;
import com.namedraw.service.AuthService;
import com.namedraw.service.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for contract tests.
 *
 * <p>This configuration provides mocked beans for services when running in test profile, allowing
 * contract tests to validate HTTP endpoints without external dependencies.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

  /**
   * Mock AuthService for contract tests.
   *
   * @return Mocked AuthService with predefined responses
   */
  @Bean
  @Primary
  public AuthService mockAuthService() {
    AuthService mockService = Mockito.mock(AuthService.class);

    // Mock valid refresh token scenario
    AuthService.AuthResponse validResponse =
        AuthService.AuthResponse.builder()
            .accessToken("new_access_token_xyz789")
            .refreshToken("new_refresh_token_def456")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .user(createTestUser())
            .build();

    try {
      Mockito.when(mockService.refreshToken("valid_refresh_token_abc123xyz"))
          .thenReturn(validResponse);
      Mockito.when(mockService.refreshToken("valid_refresh_token_def456uvw"))
          .thenReturn(validResponse);

      // Mock invalid refresh token scenarios
      Mockito.when(mockService.refreshToken("invalid_token"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Invalid refresh token"));
      Mockito.when(mockService.refreshToken("expired_token"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Token has expired"));
      Mockito.when(mockService.refreshToken("revoked_token"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Token has been revoked"));
      Mockito.when(mockService.refreshToken("malformed_token"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Malformed token"));
      Mockito.when(mockService.refreshToken("expired_refresh_token_xyz789abc"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Token has expired"));
      Mockito.when(mockService.refreshToken("invalid_refresh_token_xyz789"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Invalid refresh token"));
      Mockito.when(mockService.refreshToken("revoked_refresh_token_abc123"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Token has been revoked"));
      Mockito.when(mockService.refreshToken("not_a_jwt_token"))
          .thenThrow(new com.namedraw.exception.UnauthorizedException("Invalid token format"));
    } catch (Exception e) {
      // This shouldn't happen in mocking
    }

    return mockService;
  }

  /**
   * Mock JwtTokenService for contract tests.
   *
   * @return Mocked JwtTokenService
   */
  @Bean
  @Primary
  public JwtTokenService mockJwtTokenService() {
    JwtTokenService mockService = Mockito.mock(JwtTokenService.class);

    // Mock valid tokens
    Mockito.when(mockService.validateToken("valid_access_token_abc123")).thenReturn(true);
    Mockito.when(mockService.validateToken("valid_refresh_token_abc123xyz")).thenReturn(true);

    // Mock invalid tokens
    Mockito.when(mockService.validateToken("invalid_token")).thenReturn(false);
    Mockito.when(mockService.validateToken("expired_token")).thenReturn(false);
    Mockito.when(mockService.validateToken("revoked_token")).thenReturn(false);

    return mockService;
  }

  /**
   * Mock UserService for contract tests.
   *
   * @return Mocked UserService
   */
  @Bean
  @Primary
  public UserService mockUserService() {
    UserService mockService = Mockito.mock(UserService.class);

    UUID testUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    User testUser = createTestUser();

    Mockito.when(mockService.findById(testUserId)).thenReturn(Optional.of(testUser));

    return mockService;
  }

  private User createTestUser() {
    User user = new User();
    user.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
    user.setOauthProvider("google");
    user.setOauthId("123456789");
    user.setName("Test User");
    user.setEmail("test@example.com");
    user.setIsActive(true);
    user.setCreatedAt(LocalDateTime.now());
    user.setLastLogin(LocalDateTime.now());
    return user;
  }
}
