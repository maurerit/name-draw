package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Contract test for POST /draws/{drawId}/open endpoint
 *
 * <p>This test verifies the API contract for opening a draw for drawing. Expected to FAIL until
 * DrawController is implemented.
 *
 * <p>API Contract: - POST /api/v1/draws/{drawId}/open - Requires Bearer authentication - Path
 * parameter: drawId (UUID) - Returns 200 with Draw JSON on success - Returns 400 if cannot open
 * (wrong state, insufficient participants, etc.) - Returns 401 if unauthorized - Returns 403 if
 * forbidden (not creator) - Returns 404 if draw not found - Draw schema: {id: uuid, title: string,
 * state: string, participantCount: number, canJoin: boolean, canDraw: boolean}
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class DrawsOpenContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void openDraw_withValidCreatorRequest_shouldReturn200WithDrawJson() throws Exception {
    // Given: Valid JWT token and existing draw UUID where user is creator
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/open
    // Then: Should return 200 with Draw JSON
    sendOpenDrawRequest(validJwtToken, drawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(drawId))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.title").isString())
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.participantCount").exists())
        .andExpect(jsonPath("$.participantCount").isNumber())
        .andExpect(jsonPath("$.canJoin").value(false))
        .andExpect(jsonPath("$.canDraw").value(true))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.createdAt").isString());
  }

  @Test
  public void openDraw_withoutToken_shouldReturn401() throws Exception {
    // Given: Draw ID without authentication
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/open without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            post("/api/v1/draws/{drawId}/open", drawId).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void openDraw_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and draw ID
    String invalidJwtToken = "Bearer invalid.token.here";
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/open with invalid token
    // Then: Should return 401 Unauthorized
    sendOpenDrawRequest(invalidJwtToken, drawId)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void openDraw_withNonExistentDraw_shouldReturn404() throws Exception {
    // Given: Valid JWT token but non-existent draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String nonExistentDrawId = "999e8400-e29b-41d4-a716-446655440999";

    // When: POST /api/v1/draws/{drawId}/open with non-existent draw
    // Then: Should return 404 Not Found
    sendOpenDrawRequest(validJwtToken, nonExistentDrawId)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void openDraw_whenNotCreator_shouldReturn403() throws Exception {
    // Given: Valid JWT token but user is not the creator of the draw
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440001"; // Draw created by different user

    // When: POST /api/v1/draws/{drawId}/open when user is not creator
    // Then: Should return 403 Forbidden
    sendOpenDrawRequest(validJwtToken, drawId)
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void openDraw_whenDrawNotInJoiningState_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw that is not in JOINING state (e.g., already OPEN or ARCHIVED)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440002"; // Draw already in OPEN state

    // When: POST /api/v1/draws/{drawId}/open when draw is not in JOINING state
    // Then: Should return 400 Bad Request
    sendOpenDrawRequest(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void openDraw_withInsufficientParticipants_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw with insufficient participants (less than minimum required)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440003"; // Draw with insufficient participants

    // When: POST /api/v1/draws/{drawId}/open when draw has insufficient participants
    // Then: Should return 400 Bad Request
    sendOpenDrawRequest(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void openDraw_withInvalidDrawId_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format for drawId
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String invalidDrawId = "not-a-valid-uuid";

    // When: POST /api/v1/draws/{drawId}/open with invalid UUID
    // Then: Should return 400 Bad Request
    sendOpenDrawRequest(validJwtToken, invalidDrawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  private ResultActions sendOpenDrawRequest(String validJwtToken, String drawId) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/draws/{drawId}/open", drawId)
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON));
  }
}
