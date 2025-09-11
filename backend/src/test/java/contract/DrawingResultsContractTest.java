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
 * Contract test for GET /draws/{drawId}/results endpoint
 *
 * <p>This test verifies the API contract for retrieving draw results. Expected to FAIL until
 * DrawingController is implemented.
 *
 * <p>API Contract: - GET /api/v1/draws/{drawId}/results - Requires Bearer authentication - Path
 * parameter: drawId (UUID format) - Returns 200 with array of DrawResult JSON on success - Returns
 * 401 if unauthorized - Returns 403 if forbidden (not participant and draw not archived) - Returns
 * 404 if draw not found - DrawResult schema: {drawId: uuid, drawnUser: User, drawnAt: datetime}
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class DrawingResultsContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void getDrawResults_withValidIdAndParticipant_shouldReturn200WithResultsArray()
      throws Exception {
    // Given: Valid JWT token for participant and draw ID with results
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawIdWithResults = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId}/results
    // Then: Should return 200 with array of DrawResult JSON
    retrieveDrawResults(validJwtToken, drawIdWithResults)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].drawId").exists())
        .andExpect(jsonPath("$[0].drawId").value(drawIdWithResults))
        .andExpect(jsonPath("$[0].drawnUser").exists())
        .andExpect(jsonPath("$[0].drawnUser.id").exists())
        .andExpect(jsonPath("$[0].drawnUser.id").isString())
        .andExpect(jsonPath("$[0].drawnUser.name").exists())
        .andExpect(jsonPath("$[0].drawnUser.name").isString())
        .andExpect(jsonPath("$[0].drawnUser.isActive").exists())
        .andExpect(jsonPath("$[0].drawnUser.isActive").isBoolean())
        .andExpect(jsonPath("$[0].drawnAt").exists())
        .andExpect(jsonPath("$[0].drawnAt").isString());
  }

  @Test
  public void getDrawResults_withValidIdAndNoResults_shouldReturn200WithEmptyArray()
      throws Exception {
    // Given: Valid JWT token for participant and draw ID with no results yet
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawIdNoResults = "223e4567-e89b-12d3-a456-426614174001";

    // When: GET /api/v1/draws/{drawId}/results for draw with no results
    // Then: Should return 200 with empty array
    retrieveDrawResults(validJwtToken, drawIdNoResults)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  public void getDrawResults_withArchivedDrawAndNonParticipant_shouldReturn200WithResults()
      throws Exception {
    // Given: Valid JWT token for non-participant and archived draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5ODc2NTQzMjEwIiwibmFtZSI6IkphbmUgRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String archivedDrawId = "323e4567-e89b-12d3-a456-426614174002";

    // When: GET /api/v1/draws/{drawId}/results for archived draw (non-participant)
    // Then: Should return 200 with results (archived draws are public)
    retrieveDrawResults(validJwtToken, archivedDrawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  public void getDrawResults_withoutToken_shouldReturn401() throws Exception {
    // Given: Draw ID without authentication
    String validDrawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId}/results without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(get("/api/v1/draws/{drawId}/results", validDrawId))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDrawResults_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and draw ID
    String invalidJwtToken = "Bearer invalid.token.here";
    String validDrawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId}/results with invalid token
    // Then: Should return 401 Unauthorized
    retrieveDrawResults(invalidJwtToken, validDrawId)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDrawResults_withNonParticipantAndActiveDevice_shouldReturn403() throws Exception {
    // Given: Valid JWT token for non-participant and active (non-archived) draw
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5ODc2NTQzMjEwIiwibmFtZSI6IkphbmUgRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String activeDrawId = "423e4567-e89b-12d3-a456-426614174003";

    // When: GET /api/v1/draws/{drawId}/results for active draw as non-participant
    // Then: Should return 403 Forbidden
    retrieveDrawResults(validJwtToken, activeDrawId)
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDrawResults_withNonExistentDrawId_shouldReturn404() throws Exception {
    // Given: Valid JWT token but non-existent draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String nonExistentDrawId = "999e4567-e89b-12d3-a456-426614174999";

    // When: GET /api/v1/draws/{drawId}/results with non-existent ID
    // Then: Should return 404 Not Found
    retrieveDrawResults(validJwtToken, nonExistentDrawId)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDrawResults_withInvalidUuidFormat_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String invalidUuid = "not-a-valid-uuid";

    // When: GET /api/v1/draws/{drawId}/results with invalid UUID format
    // Then: Should return 400 Bad Request
    retrieveDrawResults(validJwtToken, invalidUuid)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDrawResults_withMultipleResults_shouldReturnAllResults() throws Exception {
    // Given: Valid JWT token for participant and draw ID with multiple results
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawIdMultipleResults = "523e4567-e89b-12d3-a456-426614174004";

    // When: GET /api/v1/draws/{drawId}/results for draw with multiple results
    // Then: Should return 200 with array containing multiple DrawResult objects
    retrieveDrawResults(validJwtToken, drawIdMultipleResults)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(1)))
        .andExpect(jsonPath("$[0].drawId").value(drawIdMultipleResults))
        .andExpect(jsonPath("$[1].drawId").value(drawIdMultipleResults))
        .andExpect(jsonPath("$[0].drawnUser.id").isString())
        .andExpect(jsonPath("$[1].drawnUser.id").isString())
        .andExpect(jsonPath("$[0].drawnAt").isString())
        .andExpect(jsonPath("$[1].drawnAt").isString());
  }

  @Test
  public void getDrawResults_withValidParticipantToken_shouldReturnResultsWithUserProfile()
      throws Exception {
    // Given: Valid JWT token for participant and draw ID with results
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawIdWithUserProfile = "623e4567-e89b-12d3-a456-426614174005";

    // When: GET /api/v1/draws/{drawId}/results
    // Then: Should return complete user profile in drawnUser
    retrieveDrawResults(validJwtToken, drawIdWithUserProfile)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].drawnUser.id").isString())
        .andExpect(jsonPath("$[0].drawnUser.name").isString())
        .andExpect(jsonPath("$[0].drawnUser.isActive").isBoolean())
        // profilePictureUrl is optional/nullable
        .andExpect(jsonPath("$[0].drawnUser").exists());
  }

  private ResultActions retrieveDrawResults(String validJwtToken, String drawIdWithResults) throws Exception {
    return mockMvc
        .perform(
            get("/api/v1/draws/{drawId}/results", drawIdWithResults)
                .header("Authorization", validJwtToken));
  }
}
