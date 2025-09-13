package com.namedraw.service;

import com.namedraw.client.AuthClient;
import com.namedraw.exception.UnauthorizedException;
import com.namedraw.model.User;
import com.namedraw.security.JwtTokenService;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service layer for authentication operations including OAuth integration.
 *
 * <p>This service handles OAuth authentication flows for Facebook and Google, JWT token management,
 * user creation/retrieval, and session management. It integrates with existing UserService for user
 * management and JwtTokenService for token operations.
 *
 * <p>Key responsibilities: - OAuth login URL generation and callback processing - JWT token
 * generation, refresh, and invalidation - User authentication and profile synchronization -
 * Integration with OAuth providers (Facebook, Google) - State parameter validation for CSRF
 * protection
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

  private final UserService userService;
  private final JwtTokenService jwtTokenService;
  private final ClientRegistrationRepository clientRegistrationRepository;
  private final AuthClient authClient;

  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String googleClientId;

  @Value("${spring.security.oauth2.client.registration.google.client-secret}")
  private String googleClientSecret;

  @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
  private String facebookClientId;

  @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
  private String facebookClientSecret;

  /**
   * Generates OAuth login URL for the specified provider.
   *
   * @param provider the OAuth provider ("google" or "facebook")
   * @param baseUrl the application base URL for redirect
   * @return the OAuth authorization URL
   * @throws IllegalArgumentException if provider is not supported
   */
  public String generateLoginUrl(String provider, String baseUrl) {
    log.debug("Generating login URL for provider: {}", provider);

    ClientRegistration clientRegistration = getClientRegistration(provider);

    String state = generateStateParameter();
    String redirectUri = baseUrl + "/api/v1/auth/callback/" + provider;

    return UriComponentsBuilder.fromUriString(
            clientRegistration.getProviderDetails().getAuthorizationUri())
        .queryParam("client_id", clientRegistration.getClientId())
        .queryParam("redirect_uri", redirectUri)
        .queryParam("scope", String.join(" ", clientRegistration.getScopes()))
        .queryParam("response_type", "code")
        .queryParam("state", state)
        .build()
        .toUriString();
  }

  /**
   * Processes OAuth callback and returns authentication response.
   *
   * @param provider the OAuth provider
   * @param code the authorization code from provider
   * @param state the state parameter for CSRF protection
   * @param baseUrl the application base URL
   * @return AuthResponse containing tokens and user info
   * @throws IllegalArgumentException if authentication fails
   */
  @Transactional
  public AuthResponse processCallback(String provider, String code, String state, String baseUrl) {
    log.debug("Processing OAuth callback for provider: {}", provider);

    // Validate state parameter (basic validation - in production use session storage)
    if (state == null || state.trim().isEmpty()) {
      throw new IllegalArgumentException("Invalid state parameter");
    }

    // Exchange code for access token
    String accessToken = exchangeCodeForToken(provider, code, baseUrl);

    // Get user info from provider
    UserInfo userInfo = getUserInfoFromProvider(provider, accessToken);

    // Create or update user
    User user = createOrUpdateUser(provider, userInfo);

    // Generate JWT tokens
    String jwtAccessToken = jwtTokenService.generateAccessToken(user);
    String jwtRefreshToken = jwtTokenService.generateRefreshToken(user);

    log.info("Successfully authenticated user: {} via {}", user.getEmail(), provider);

    return AuthResponse.builder()
        .accessToken(jwtAccessToken)
        .refreshToken(jwtRefreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtTokenService.getExpirationTime("access"))
        .user(user)
        .build();
  }

  /**
   * Refreshes JWT tokens using refresh token.
   *
   * @param refreshToken the refresh token
   * @return new AuthResponse with refreshed tokens
   * @throws IllegalArgumentException if refresh token is invalid
   */
  @Transactional
  public AuthResponse refreshToken(String refreshToken) {
    log.debug("Refreshing JWT token");

    if (!jwtTokenService.validateToken(refreshToken)) {
      throw new UnauthorizedException("Invalid refresh token");
    }

    if (!jwtTokenService.isTokenType(refreshToken, "refresh")) {
      throw new UnauthorizedException("Token is not a refresh token");
    }

    // Get user from token
    var userId = jwtTokenService.getUserIdFromToken(refreshToken);
    Optional<User> userOpt = userService.findById(userId);

    if (userOpt.isEmpty()) {
      throw new UnauthorizedException("User not found");
    }

    User user = userOpt.get();

    // Invalidate old refresh token
    jwtTokenService.invalidateToken(refreshToken);

    // Generate new tokens
    String newAccessToken = jwtTokenService.generateAccessToken(user);
    String newRefreshToken = jwtTokenService.generateRefreshToken(user);

    log.info("Successfully refreshed tokens for user: {}", user.getEmail());

    return AuthResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtTokenService.getExpirationTime("access"))
        .user(user)
        .build();
  }

  /**
   * Logs out user by invalidating their tokens.
   *
   * @param accessToken the access token to invalidate
   */
  public void logout(String accessToken) {
    log.debug("Logging out user");

    if (jwtTokenService.validateToken(accessToken)) {
      jwtTokenService.invalidateToken(accessToken);
      log.info("Successfully logged out user");
    }
  }

  private ClientRegistration getClientRegistration(String provider) {
    ClientRegistration clientRegistration =
        clientRegistrationRepository.findByRegistrationId(provider);
    if (clientRegistration == null) {
      throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
    }
    return clientRegistration;
  }

  private String generateStateParameter() {
    // In production, this should be stored in session and validated
    return java.util.UUID.randomUUID().toString();
  }

  private String exchangeCodeForToken(String provider, String code, String baseUrl) {
    ClientRegistration clientRegistration = getClientRegistration(provider);
    String redirectUri = baseUrl + "/api/v1/auth/callback/" + provider;

    try {
      Map<String, Object> responseBody =
          authClient.exchangeCodeForToken(
              clientRegistration.getProviderDetails().getTokenUri(),
              clientRegistration.getClientId(),
              clientRegistration.getClientSecret(),
              code,
              redirectUri);

      if (!responseBody.containsKey("access_token")) {
        throw new IllegalArgumentException("Failed to obtain access token from " + provider);
      }

      return (String) responseBody.get("access_token");
    } catch (Exception e) {
      log.error("Error exchanging code for token with {}: {}", provider, e.getMessage());
      throw new IllegalArgumentException("Failed to exchange authorization code for token");
    }
  }

  private UserInfo getUserInfoFromProvider(String provider, String accessToken) {
    ClientRegistration clientRegistration = getClientRegistration(provider);

    try {
      Map<String, Object> userAttributes =
          authClient.getUserInfo(
              clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri(), accessToken);

      return parseUserInfo(provider, userAttributes);
    } catch (Exception e) {
      log.error("Error getting user info from {}: {}", provider, e.getMessage());
      throw new IllegalArgumentException("Failed to get user information from provider");
    }
  }

  private UserInfo parseUserInfo(String provider, Map<String, Object> attributes) {
    String oauthId;
    String email;
    String name;
    String pictureUrl = null;

    if ("google".equals(provider)) {
      oauthId = (String) attributes.get("sub");
      email = (String) attributes.get("email");
      name = (String) attributes.get("name");
      pictureUrl = (String) attributes.get("picture");
    } else if ("facebook".equals(provider)) {
      oauthId = (String) attributes.get("id");
      email = (String) attributes.get("email");
      name = (String) attributes.get("name");
      @SuppressWarnings("unchecked")
      Map<String, Object> picture = (Map<String, Object>) attributes.get("picture");
      if (picture != null && picture.containsKey("data")) {
        @SuppressWarnings("unchecked")
        Map<String, Object> pictureData = (Map<String, Object>) picture.get("data");
        pictureUrl = (String) pictureData.get("url");
      }
    } else {
      throw new IllegalArgumentException("Unsupported provider: " + provider);
    }

    if (oauthId == null || email == null || name == null) {
      throw new IllegalArgumentException("Incomplete user information from " + provider);
    }

    return UserInfo.builder()
        .oauthId(oauthId)
        .email(email)
        .name(name)
        .pictureUrl(pictureUrl)
        .build();
  }

  @Transactional
  private User createOrUpdateUser(String provider, UserInfo userInfo) {
    // Use the existing UserService method that handles both create and update
    return userService.createOrUpdateUser(
        provider,
        userInfo.getOauthId(),
        userInfo.getName(),
        userInfo.getEmail(),
        userInfo.getPictureUrl());
  }

  /** Data transfer object for OAuth user information. */
  @lombok.Builder
  @lombok.Data
  private static class UserInfo {
    private String oauthId;
    private String email;
    private String name;
    private String pictureUrl;
  }

  /** Response object for authentication operations. */
  @lombok.Builder
  @lombok.Data
  public static class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private User user;
  }
}
