package com.namedraw.controller;

import com.namedraw.security.JwtTokenService;
import com.namedraw.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations including OAuth flows.
 *
 * <p>This controller handles OAuth authentication flows for Facebook and Google providers, JWT
 * token management, and user session management. It delegates business logic to AuthService and
 * returns appropriate HTTP responses according to the OpenAPI specification.
 *
 * <p>Endpoints: - GET /auth/login/{provider}: Initiate OAuth login - POST
 * /auth/callback/{provider}: Handle OAuth callback - POST /auth/refresh: Refresh JWT token - POST
 * /auth/logout: Logout and invalidate token
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final AuthService authService;
  private final JwtTokenService jwtTokenService;

  /**
   * Initiate OAuth login with specified provider.
   *
   * @param provider OAuth provider (facebook or google)
   * @param request HTTP request to get base URL
   * @return Redirect response to OAuth provider authorization URL
   */
  @GetMapping("/login/{provider}")
  public ResponseEntity<Void> login(@PathVariable String provider, HttpServletRequest request) {
    log.info("Initiating OAuth login for provider: {}", provider);

    try {
      String baseUrl = getBaseUrl(request);
      String authorizationUrl = authService.generateLoginUrl(provider, baseUrl);
      return ResponseEntity.status(HttpStatus.FOUND).header("Location", authorizationUrl).build();
    } catch (IllegalArgumentException e) {
      log.warn("Invalid OAuth provider requested: {}", provider);
      throw new IllegalArgumentException("Invalid provider: " + provider);
    }
  }

  /**
   * Handle OAuth callback from provider.
   *
   * @param provider OAuth provider (facebook or google)
   * @param callbackRequest Request containing authorization code and state
   * @param request HTTP request to get base URL
   * @return Authentication response with JWT tokens and user info
   */
  @PostMapping("/callback/{provider}")
  public ResponseEntity<Map<String, Object>> callback(
      @PathVariable String provider,
      @RequestBody Map<String, String> callbackRequest,
      HttpServletRequest request) {
    log.info("Processing OAuth callback for provider: {}", provider);

    String code = callbackRequest.get("code");
    String state = callbackRequest.get("state");

    if (code == null || state == null) {
      log.warn("Missing required parameters in OAuth callback");
      return ResponseEntity.badRequest().build();
    }

    try {
      String baseUrl = getBaseUrl(request);
      AuthService.AuthResponse authResponse =
          authService.processCallback(provider, code, state, baseUrl);
      Map<String, Object> response = convertAuthResponse(authResponse);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid provider in callback: {}", provider);
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("OAuth callback processing failed for provider: {}", provider, e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  /**
   * Refresh JWT token using refresh token.
   *
   * @param refreshRequest Request containing refresh token
   * @return New authentication response with refreshed tokens
   */
  @PostMapping("/refresh")
  public ResponseEntity<Map<String, Object>> refresh(
      @RequestBody Map<String, String> refreshRequest) {
    log.info("Processing token refresh request");

    String refreshToken = refreshRequest.get("refreshToken");

    if (refreshToken == null || refreshToken.trim().isEmpty()) {
      log.warn("Missing or empty refresh token in refresh request");
      throw new IllegalArgumentException("Refresh token is required");
    }

    try {
      AuthService.AuthResponse authResponse = authService.refreshToken(refreshToken);
      Map<String, Object> response = convertAuthResponse(authResponse);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Token refresh failed", e);
      throw e; // Let GlobalExceptionHandler handle it
    }
  }

  /**
   * Logout user and invalidate JWT token.
   *
   * @param jwt Current user's JWT token (can be null if token is invalid)
   * @return Success response
   */
  @PostMapping("/logout")
  public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
      log.warn("Logout attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              Map.of(
                  "error", "Unauthorized",
                  "message", "Invalid or missing JWT token",
                  "timestamp", Instant.now().toString(),
                  "path", "/api/v1/auth/logout"));
    }

    // Get the raw token value and invalidate it
    String tokenValue = jwt.getTokenValue();
    jwtTokenService.invalidateToken(tokenValue);
    log.info("User logged out and token invalidated");

    return ResponseEntity.ok(Map.of("message", "Logout successful"));
  }

  /**
   * Extract base URL from HTTP request.
   *
   * @param request HTTP servlet request
   * @return Base URL for the application
   */
  private String getBaseUrl(HttpServletRequest request) {
    String scheme = request.getScheme();
    String serverName = request.getServerName();
    int serverPort = request.getServerPort();
    String contextPath = request.getContextPath();

    StringBuilder baseUrl = new StringBuilder();
    baseUrl.append(scheme).append("://").append(serverName);

    if (("http".equals(scheme) && serverPort != 80)
        || ("https".equals(scheme) && serverPort != 443)) {
      baseUrl.append(":").append(serverPort);
    }

    baseUrl.append(contextPath);
    return baseUrl.toString();
  }

  /**
   * Convert AuthService.AuthResponse to Map for JSON serialization.
   *
   * @param authResponse AuthService response object
   * @return Map representation for JSON response
   */
  private Map<String, Object> convertAuthResponse(AuthService.AuthResponse authResponse) {
    Map<String, Object> response = new HashMap<>();
    response.put("accessToken", authResponse.getAccessToken());
    response.put("refreshToken", authResponse.getRefreshToken());
    response.put("tokenType", authResponse.getTokenType());
    response.put("expiresIn", authResponse.getExpiresIn());
    response.put("user", authResponse.getUser());
    return response;
  }
}
