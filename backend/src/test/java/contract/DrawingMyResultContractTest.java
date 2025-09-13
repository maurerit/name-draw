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
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contract test for GET /draws/{drawId}/my-result endpoint
 *
 * <p>This test verifies the API contract for retrieving the authenticated user's draw result.
 * Expected to FAIL until DrawingController is implemented.
 *
 * <p>API Contract: - GET /api/v1/draws/{drawId}/my-result - Requires Bearer authentication - Path
 * parameter: drawId (UUID format) - Returns 200 with DrawResult JSON on success - Returns 401 if
 * unauthorized - Returns 403 if forbidden (not participant) - Returns 404 if draw not found or user
 * hasn't drawn yet - DrawResult schema: {drawId: uuid, drawnUser: User, drawnAt: datetime}
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawingMyResultContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void getMyDrawResult_withValidParticipantAndResult_shouldReturn200WithDrawResult()
      throws Exception {
    // Given: Valid JWT token for participant and draw ID where user has drawn
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawIdWithResult = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId}/my-result
    // Then: Should return 200 with DrawResult JSON
    executeDrawResultFetch(validJwtToken, drawIdWithResult)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.drawId").exists())
        .andExpect(jsonPath("$.drawId").value(drawIdWithResult))
        .andExpect(jsonPath("$.drawnUser").exists())
        .andExpect(jsonPath("$.drawnUser.id").exists())
        .andExpect(jsonPath("$.drawnUser.id").isString())
        .andExpect(jsonPath("$.drawnUser.name").exists())
        .andExpect(jsonPath("$.drawnUser.name").isString())
        .andExpect(jsonPath("$.drawnUser.isActive").exists())
        .andExpect(jsonPath("$.drawnUser.isActive").isBoolean())
        .andExpect(jsonPath("$.drawnAt").exists())
        .andExpect(jsonPath("$.drawnAt").isString());
  }

  @Test
  public void getMyDrawResult_withoutToken_shouldReturn401() throws Exception {
    // Given: Draw ID without authentication
    String validDrawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId}/my-result without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(get("/api/v1/draws/{drawId}/my-result", validDrawId))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getMyDrawResult_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and draw ID
    String invalidJwtToken = "Bearer invalid.token.here";
    String validDrawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId}/my-result with invalid token
    // Then: Should return 401 Unauthorized
    executeDrawResultFetch(invalidJwtToken, validDrawId)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getMyDrawResult_withNonParticipant_shouldReturn403() throws Exception {
    // Given: Valid JWT token for non-participant user
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5ODc2NTQzMjEwIiwibmFtZSI6IkphbmUgRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawId = "423e4567-e89b-12d3-a456-426614174003";

    // When: GET /api/v1/draws/{drawId}/my-result as non-participant
    // Then: Should return 403 Forbidden
    executeDrawResultFetch(validJwtToken, drawId)
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getMyDrawResult_withNonExistentDrawId_shouldReturn404() throws Exception {
    // Given: Valid JWT token but non-existent draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String nonExistentDrawId = "999e4567-e89b-12d3-a456-426614174999";

    // When: GET /api/v1/draws/{drawId}/my-result with non-existent ID
    // Then: Should return 404 Not Found
    executeDrawResultFetch(validJwtToken, nonExistentDrawId)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getMyDrawResult_withParticipantButNoResultYet_shouldReturn404() throws Exception {
    // Given: Valid JWT token for participant but user hasn't drawn yet
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawIdNoResult = "223e4567-e89b-12d3-a456-426614174001";

    // When: GET /api/v1/draws/{drawId}/my-result for participant who hasn't drawn
    // Then: Should return 404 Not Found
    executeDrawResultFetch(validJwtToken, drawIdNoResult)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getMyDrawResult_withInvalidUuidFormat_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String invalidUuid = "not-a-valid-uuid";

    // When: GET /api/v1/draws/{drawId}/my-result with invalid UUID format
    // Then: Should return 400 Bad Request
    executeDrawResultFetch(validJwtToken, invalidUuid)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getMyDrawResult_withValidParticipantToken_shouldReturnResultWithCompleteUserProfile()
      throws Exception {
    // Given: Valid JWT token for participant and draw ID with result
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawIdWithUserProfile = "623e4567-e89b-12d3-a456-426614174005";

    // When: GET /api/v1/draws/{drawId}/my-result
    // Then: Should return complete user profile in drawnUser
    executeDrawResultFetch(validJwtToken, drawIdWithUserProfile)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.drawId").value(drawIdWithUserProfile))
        .andExpect(jsonPath("$.drawnUser.id").isString())
        .andExpect(jsonPath("$.drawnUser.name").isString())
        .andExpect(jsonPath("$.drawnUser.isActive").isBoolean())
        // profilePictureUrl is optional/nullable
        .andExpect(jsonPath("$.drawnUser").exists())
        .andExpect(jsonPath("$.drawnAt").isString());
  }

  private ResultActions executeDrawResultFetch(String validJwtToken, String drawIdWithResult)
      throws Exception {
    return mockMvc.perform(
        get("/api/v1/draws/{drawId}/my-result", drawIdWithResult)
            .header("Authorization", validJwtToken));
  }
}
