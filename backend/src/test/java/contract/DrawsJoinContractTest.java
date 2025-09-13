package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contract test for POST /draws/{drawId}/join endpoint
 *
 * <p>This test verifies the API contract for joining a draw as a participant. Expected to FAIL
 * until DrawController is implemented.
 *
 * <p>API Contract: - POST /api/v1/draws/{drawId}/join - Requires Bearer authentication - Path
 * parameter: drawId (UUID) - Returns 200 with Participation JSON on success - Returns 400 if cannot
 * join (wrong state, already joined, etc.) - Returns 401 if unauthorized - Returns 404 if draw not
 * found - Participation schema: {id: uuid, draw: Draw, joinedAt: datetime}
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawsJoinContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void joinDraw_withValidRequest_shouldReturn200WithParticipationJson() throws Exception {
    // Given: Valid JWT token and existing draw UUID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/join
    // Then: Should return 200 with Participation JSON
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.draw").exists())
        .andExpect(jsonPath("$.draw.id").value(drawId))
        .andExpect(jsonPath("$.draw.title").exists())
        .andExpect(jsonPath("$.draw.state").exists())
        .andExpect(jsonPath("$.draw.participantCount").exists())
        .andExpect(jsonPath("$.draw.canJoin").exists())
        .andExpect(jsonPath("$.draw.canDraw").exists())
        .andExpect(jsonPath("$.joinedAt").exists())
        .andExpect(jsonPath("$.joinedAt").isString());
  }

  @Test
  public void joinDraw_withoutToken_shouldReturn401() throws Exception {
    // Given: Draw ID without authentication
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/join without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            post("/api/v1/draws/{drawId}/join", drawId).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and draw ID
    String invalidJwtToken = "Bearer invalid.token.here";
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/join with invalid token
    // Then: Should return 401 Unauthorized
    joinDrawWithToken(invalidJwtToken, drawId)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_withNonExistentDraw_shouldReturn404() throws Exception {
    // Given: Valid JWT token but non-existent draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String nonExistentDrawId = "999e8400-e29b-41d4-a716-446655440999";

    // When: POST /api/v1/draws/{drawId}/join with non-existent draw
    // Then: Should return 404 Not Found
    joinDrawWithToken(validJwtToken, nonExistentDrawId)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenAlreadyJoined_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw where user is already a participant
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440001"; // Draw user already joined

    // When: POST /api/v1/draws/{drawId}/join when already joined
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenDrawNotInJoiningState_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw that is not in JOINING state (e.g., OPEN or ARCHIVED)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440002"; // Draw in OPEN state

    // When: POST /api/v1/draws/{drawId}/join when draw is not in JOINING state
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenDrawAtMaxCapacity_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw that has reached maximum participants
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440003"; // Draw at max capacity

    // When: POST /api/v1/draws/{drawId}/join when draw is at max capacity
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenUserIsCreator_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw where the user is the creator
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440004"; // Draw created by this user

    // When: POST /api/v1/draws/{drawId}/join when user is the creator
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_withInvalidDrawId_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format for drawId
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String invalidDrawId = "not-a-valid-uuid";

    // When: POST /api/v1/draws/{drawId}/join with invalid UUID
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, invalidDrawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  private ResultActions joinDrawWithToken(String validJwtToken, String drawId) throws Exception {
    return mockMvc.perform(
        post("/api/v1/draws/{drawId}/join", drawId)
            .header("Authorization", validJwtToken)
            .contentType(MediaType.APPLICATION_JSON));
  }
}
