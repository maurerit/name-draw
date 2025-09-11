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
 * Contract test for POST /draws/{drawId}/draw endpoint
 *
 * <p>This test verifies the API contract for performing a name draw. Expected to FAIL until
 * DrawingController is implemented.
 *
 * <p>API Contract: - POST /api/v1/draws/{drawId}/draw - Requires Bearer authentication - Path
 * parameter: drawId (UUID) - Returns 200 with DrawResult JSON on success - Returns 400 if cannot
 * draw (wrong state, already drawn, etc.) - Returns 401 if unauthorized - Returns 403 if forbidden
 * (not participant) - Returns 404 if draw not found - Returns 409 if conflict (another draw in
 * progress) - DrawResult schema: {drawId: uuid, drawnUser: User, drawnAt: datetime}
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class DrawingPerformContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void performDraw_withValidRequest_shouldReturn200WithDrawResultJson() throws Exception {
    // Given: Valid JWT token and existing draw UUID in OPEN state with participants
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/draw
    // Then: Should return 200 with DrawResult JSON
    sendDrawRequest(validJwtToken, drawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.drawId").exists())
        .andExpect(jsonPath("$.drawId").value(drawId))
        .andExpect(jsonPath("$.drawnUser").exists())
        .andExpect(jsonPath("$.drawnUser.id").exists())
        .andExpect(jsonPath("$.drawnUser.id").isString())
        .andExpect(jsonPath("$.drawnUser.name").exists())
        .andExpect(jsonPath("$.drawnUser.name").isString())
        .andExpect(jsonPath("$.drawnUser.email").exists())
        .andExpect(jsonPath("$.drawnUser.email").isString())
        .andExpect(jsonPath("$.drawnAt").exists())
        .andExpect(jsonPath("$.drawnAt").isString());
  }

  @Test
  public void performDraw_withoutToken_shouldReturn401() throws Exception {
    // Given: Draw ID without authentication
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/draw without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            post("/api/v1/draws/{drawId}/draw", drawId).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and draw ID
    String invalidJwtToken = "Bearer invalid.token.here";
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/draw with invalid token
    // Then: Should return 401 Unauthorized
    sendDrawRequest(invalidJwtToken, drawId)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_withNonExistentDraw_shouldReturn404() throws Exception {
    // Given: Valid JWT token but non-existent draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String nonExistentDrawId = "999e8400-e29b-41d4-a716-446655440999";

    // When: POST /api/v1/draws/{drawId}/draw with non-existent draw
    // Then: Should return 404 Not Found
    sendDrawRequest(validJwtToken, nonExistentDrawId)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_whenUserNotParticipant_shouldReturn403() throws Exception {
    // Given: Valid JWT token and draw where user is not a participant
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440001"; // Draw user is not participant of

    // When: POST /api/v1/draws/{drawId}/draw when user is not a participant
    // Then: Should return 403 Forbidden
    sendDrawRequest(validJwtToken, drawId)
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_whenDrawNotInOpenState_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw that is not in OPEN state (e.g., JOINING or ARCHIVED)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440002"; // Draw in JOINING state

    // When: POST /api/v1/draws/{drawId}/draw when draw is not in OPEN state
    // Then: Should return 400 Bad Request
    sendDrawRequest(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_whenUserAlreadyDrewName_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw where user has already performed a draw
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440003"; // Draw where user already drew

    // When: POST /api/v1/draws/{drawId}/draw when user already drew a name
    // Then: Should return 400 Bad Request
    sendDrawRequest(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_whenAnotherDrawInProgress_shouldReturn409() throws Exception {
    // Given: Valid JWT token and draw where another participant is currently drawing
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440004"; // Draw with concurrent access

    // When: POST /api/v1/draws/{drawId}/draw when another draw is in progress
    // Then: Should return 409 Conflict
    sendDrawRequest(validJwtToken, drawId)
        .andExpect(status().isConflict())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_withInvalidDrawId_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format for drawId
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String invalidDrawId = "not-a-valid-uuid";

    // When: POST /api/v1/draws/{drawId}/draw with invalid UUID
    // Then: Should return 400 Bad Request
    sendDrawRequest(validJwtToken, invalidDrawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void performDraw_whenNoNamesLeftToDraw_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw where all names have been drawn
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "550e8400-e29b-41d4-a716-446655440005"; // Draw with no names left

    // When: POST /api/v1/draws/{drawId}/draw when no names are left to draw
    // Then: Should return 400 Bad Request
    sendDrawRequest(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  private ResultActions sendDrawRequest(String validJwtToken, String drawId) throws Exception {
    return mockMvc
        .perform(
            post("/api/v1/draws/{drawId}/draw", drawId)
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON));
  }
}
