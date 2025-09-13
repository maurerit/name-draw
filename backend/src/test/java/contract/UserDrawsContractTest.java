package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract test for GET /users/me/draws endpoint
 *
 * <p>This test verifies the API contract for getting the current user's draws. Expected to FAIL
 * until UserController is implemented.
 *
 * <p>API Contract: - GET /api/v1/users/me/draws - Query parameters: role=[creator|participant|all],
 * state=[JOINING|OPEN|ARCHIVED|all] - Requires Bearer authentication - Returns 200 with Draw array
 * on success - Returns 401 if unauthorized - Draw schema: {id, title, description?, state,
 * drawDate, participantCount, maxParticipants, creator, participants, canJoin, canDraw, createdAt,
 * openedAt?, archivedAt?}
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserDrawsContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void getUserDraws_withValidToken_shouldReturn200WithDrawArray() throws Exception {
    // Given: Valid JWT token for authenticated user
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/users/me/draws
    // Then: Should return 200 with Draw array
    mockMvc
        .perform(
            get("/api/v1/users/me/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  public void getUserDraws_withoutToken_shouldReturn401() throws Exception {
    // When: GET /api/v1/users/me/draws without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(get("/api/v1/users/me/draws").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getUserDraws_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token
    String invalidJwtToken = "Bearer invalid.jwt.token";

    // When: GET /api/v1/users/me/draws with invalid token
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            get("/api/v1/users/me/draws")
                .header("Authorization", invalidJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }
}
