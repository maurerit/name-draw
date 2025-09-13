package com.namedraw.security;

import com.namedraw.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for JWT token management including generation, validation, refresh, and invalidation.
 *
 * <p>This service handles all JWT-related operations for the Name Draw application, including: -
 * Access token generation and validation - Refresh token management - Token invalidation (logout) -
 * Claims extraction and user identification
 *
 * <p>Tokens are signed using HMAC-SHA256 with a configurable secret key. The service maintains a
 * blacklist of invalidated tokens to support logout functionality.
 */
@Service
@Slf4j
public class JwtTokenService {

  private final long jwtExpirationMs;
  private final long refreshExpirationMs;
  private final SecretKey signingKey;

  // In-memory token blacklist (in production, use Redis or similar)
  private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

  /**
   * Constructor to initialize JwtTokenService with configuration properties.
   *
   * @param jwtSecret the secret key for signing tokens
   * @param jwtExpirationMs the expiration time for JWT tokens in milliseconds
   */
  public JwtTokenService(
      @Value("${jwt.secret}") String jwtSecret,
      @Value("${jwt.expiration:86400000}") long jwtExpirationMs) {
    this.jwtExpirationMs = jwtExpirationMs;
    this.refreshExpirationMs = jwtExpirationMs * 7; // 7 times longer for refresh tokens
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Generates an access token for the given user.
   *
   * @param user the user to generate the token for
   * @return the JWT access token
   */
  public String generateAccessToken(User user) {
    return generateToken(user, jwtExpirationMs, "access");
  }

  /**
   * Generates a refresh token for the given user.
   *
   * @param user the user to generate the token for
   * @return the JWT refresh token
   */
  public String generateRefreshToken(User user) {
    return generateToken(user, refreshExpirationMs, "refresh");
  }

  /**
   * Validates a JWT token and returns true if valid.
   *
   * @param token the JWT token to validate
   * @return true if the token is valid, false otherwise
   */
  public boolean validateToken(String token) {
    if (blacklistedTokens.contains(token)) {
      log.debug("Token is blacklisted");
      return false;
    }

    try {
      Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
      return true;
    } catch (MalformedJwtException e) {
      log.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("JWT claims string is empty: {}", e.getMessage());
    }
    return false;
  }

  /**
   * Extracts the user ID from a JWT token.
   *
   * @param token the JWT token
   * @return the user ID
   * @throws IllegalArgumentException if the token is invalid
   */
  public UUID getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();

    String userId = claims.getSubject();
    return UUID.fromString(userId);
  }

  /**
   * Extracts the token type from a JWT token.
   *
   * @param token the JWT token
   * @return the token type ("access" or "refresh")
   */
  public String getTokenType(String token) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();

    return claims.get("type", String.class);
  }

  /**
   * Invalidates a token by adding it to the blacklist.
   *
   * @param token the token to invalidate
   */
  public void invalidateToken(String token) {
    blacklistedTokens.add(token);
    log.debug("Token added to blacklist");
  }

  /**
   * Checks if a token is of the specified type.
   *
   * @param token the JWT token
   * @param expectedType the expected token type
   * @return true if the token type matches, false otherwise
   */
  public boolean isTokenType(String token, String expectedType) {
    try {
      String tokenType = getTokenType(token);
      return expectedType.equals(tokenType);
    } catch (Exception e) {
      log.error("Error checking token type: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Gets the expiration time of a token in seconds from now.
   *
   * @param tokenType the type of token ("access" or "refresh")
   * @return expiration time in seconds
   */
  public long getExpirationTime(String tokenType) {
    if ("refresh".equals(tokenType)) {
      return refreshExpirationMs / 1000;
    }
    return jwtExpirationMs / 1000;
  }

  private String generateToken(User user, long expirationMs, String tokenType) {
    Instant now = Instant.now();
    final Instant expiration = now.plusMillis(expirationMs);

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", tokenType);
    claims.put("userId", user.getId().toString());
    claims.put("email", user.getEmail());
    claims.put("name", user.getName());

    return Jwts.builder()
        .subject(user.getId().toString())
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(signingKey)
        .compact();
  }
}
