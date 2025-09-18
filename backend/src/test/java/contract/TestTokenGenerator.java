package contract;

import com.namedraw.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

/** Utility class for generating test JWT tokens for contract tests. */
public class TestTokenGenerator {

  private static final String TEST_JWT_SECRET =
      "test-jwt-secret-key-for-contract-tests-minimum-256-bits-long";
  private static final SecretKey SIGNING_KEY =
      Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
  // Deterministic different user id for tests that need a non-creator identity
  private static final String SECOND_USER_ID = "223e4567-e89b-12d3-a456-426614174001";

  /** Generates a valid refresh token for testing. */
  public static String generateValidRefreshToken() {
    User testUser = ContractTestConfig.getTestUser();
    if (testUser == null) {
      throw new IllegalStateException(
          "Test user not initialized. Make sure ContractTestConfig is loaded.");
    }

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    claims.put("userId", testUser.getId().toString());

    Instant now = Instant.now();
    Instant expiry = now.plus(7, ChronoUnit.DAYS); // 7 days for refresh tokens

    return Jwts.builder()
        .subject(testUser.getId().toString())
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(SIGNING_KEY, Jwts.SIG.HS384)
        .compact();
  }

  /** Generates another valid refresh token (different from the first) for testing. */
  public static String generateSecondValidRefreshToken() {
    User testUser = ContractTestConfig.getTestUser();
    if (testUser == null) {
      throw new IllegalStateException(
          "Test user not initialized. Make sure ContractTestConfig is loaded.");
    }

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    claims.put("userId", testUser.getId().toString());
    claims.put("version", "2"); // Add a version to make it different

    Instant now = Instant.now();
    Instant expiry = now.plus(7, ChronoUnit.DAYS);

    return Jwts.builder()
        .subject(testUser.getId().toString())
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(SIGNING_KEY, Jwts.SIG.HS384)
        .compact();
  }

  /** Generates an expired refresh token for testing. */
  public static String generateExpiredRefreshToken() {
    User testUser = ContractTestConfig.getTestUser();
    if (testUser == null) {
      throw new IllegalStateException(
          "Test user not initialized. Make sure ContractTestConfig is loaded.");
    }

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    claims.put("userId", testUser.getId().toString());

    Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
    Instant expiry = past.plus(1, ChronoUnit.HOURS); // Expired 23 hours ago

    return Jwts.builder()
        .subject(testUser.getId().toString())
        .claims(claims)
        .issuedAt(Date.from(past))
        .expiration(Date.from(expiry))
        .signWith(SIGNING_KEY, Jwts.SIG.HS384)
        .compact();
  }

  /** Generates a valid access token for testing (wrong type for refresh endpoint). */
  public static String generateValidAccessToken() {
    User testUser = ContractTestConfig.getTestUser();
    if (testUser == null) {
      throw new IllegalStateException(
          "Test user not initialized. Make sure ContractTestConfig is loaded.");
    }

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "access");
    claims.put("userId", testUser.getId().toString());

    Instant now = Instant.now();
    Instant expiry = now.plus(1, ChronoUnit.HOURS);

    return Jwts.builder()
        .subject(testUser.getId().toString())
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(SIGNING_KEY, Jwts.SIG.HS384)
        .compact();
  }

  /** Generates a valid access token for a different (non-test) user. */
  public static String generateValidAccessTokenForDifferentUser() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "access");
    claims.put("userId", SECOND_USER_ID);

    Instant now = Instant.now();
    Instant expiry = now.plus(1, ChronoUnit.HOURS);

    return Jwts.builder()
        .subject(SECOND_USER_ID)
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(SIGNING_KEY, Jwts.SIG.HS384)
        .compact();
  }

  /** Returns a malformed token string. */
  public static String getMalformedToken() {
    return "invalid.malformed.token";
  }

  /**
   * Generates a valid refresh token that should be treated as revoked. This token is valid in
   * structure but should be added to blacklist for testing.
   */
  public static String generateRevokedRefreshToken() {
    User testUser = ContractTestConfig.getTestUser();
    if (testUser == null) {
      throw new IllegalStateException(
          "Test user not initialized. Make sure ContractTestConfig is loaded.");
    }

    Map<String, Object> claims = new HashMap<>();
    claims.put("type", "refresh");
    claims.put("userId", testUser.getId().toString());
    claims.put("revoked", "true"); // Mark it as revoked for identification

    Instant now = Instant.now();
    Instant expiry = now.plus(7, ChronoUnit.DAYS);

    return Jwts.builder()
        .subject(testUser.getId().toString())
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(SIGNING_KEY, Jwts.SIG.HS384)
        .compact();
  }
}
