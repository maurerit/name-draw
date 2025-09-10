package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contract test for GET /users/me endpoint
 *
 * <p>This test verifies the API contract for getting the current user's profile. Expected to FAIL
 * until UserController is implemented.
 *
 * <p>API Contract: - GET /api/v1/users/me - Requires Bearer authentication - Returns 200 with User
 * JSON on success - Returns 401 if unauthorized - User schema: {id: uuid, name: string,
 * profilePictureUrl: string|null, isActive: boolean}
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class UserProfileContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void getUserProfile_withValidToken_shouldReturn200WithUserData() throws Exception {
    // Given: Valid JWT token for authenticated user
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/users/me
    // Then: Should return 200 with User JSON
    retrieveUserProfile(validJwtToken)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.name").isString())
        .andExpect(jsonPath("$.profilePictureUrl").exists()) // can be null
        .andExpect(jsonPath("$.isActive").exists())
        .andExpect(jsonPath("$.isActive").isBoolean());
  }

  @Test
  public void getUserProfile_withoutToken_shouldReturn401() throws Exception {
    // When: GET /api/v1/users/me without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(get("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getUserProfile_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token
    String invalidJwtToken = "Bearer invalid.jwt.token";

    // When: GET /api/v1/users/me with invalid token
    // Then: Should return 401 Unauthorized
    retrieveUserProfile(invalidJwtToken)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getUserProfile_withMalformedAuthHeader_shouldReturn401() throws Exception {
    // Given: Malformed Authorization header (missing Bearer prefix)
    String malformedHeader = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid";

    // When: GET /api/v1/users/me with malformed header
    // Then: Should return 401 Unauthorized
    retrieveUserProfile(malformedHeader)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getUserProfile_withExpiredToken_shouldReturn401() throws Exception {
    // Given: Expired JWT token
    String expiredJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid_expired_signature";

    // When: GET /api/v1/users/me with expired token
    // Then: Should return 401 Unauthorized
    retrieveUserProfile(expiredJwtToken)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getUserProfile_deactivatedUser_shouldReturn401() throws Exception {
    // Given: Valid JWT token for deactivated user
    String deactivatedUserToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJkZWFjdGl2YXRlZC11c2VyLWlkIiwibmFtZSI6IkRlYWN0aXZhdGVkIFVzZXIiLCJpc0FjdGl2ZSI6ZmFsc2V9.signature";

    // When: GET /api/v1/users/me with deactivated user token
    // Then: Should return 401 Unauthorized (deactivated users cannot access API)
    retrieveUserProfile(deactivatedUserToken)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  private ResultActions retrieveUserProfile(String validJwtToken) throws Exception {
    return mockMvc
        .perform(
            get("/api/v1/users/me")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON));
  }
}
