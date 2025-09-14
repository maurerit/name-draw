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
 * Contract test for GET /draws/{drawId} endpoint
 *
 * <p>This test verifies the API contract for retrieving a specific draw. Expected to FAIL until
 * DrawController is implemented.
 *
 * <p>API Contract: - GET /api/v1/draws/{drawId} - Requires Bearer authentication - Path parameter:
 * drawId (UUID format) - Returns 200 with Draw JSON on success - Returns 401 if unauthorized -
 * Returns 404 if draw not found - Draw schema: {id: uuid, title: string, description?: string,
 * state: enum, drawDate: date, participantCount: number, maxParticipants: number, creator: User,
 * participants: User[], canJoin: boolean, canDraw: boolean, createdAt: datetime, openedAt?:
 * datetime, archivedAt?: datetime}
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawsGetContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void getDraw_withValidIdAndAuth_shouldReturn200WithDrawJson() throws Exception {
    // Given: Valid JWT token and existing draw ID
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String validDrawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId}
    // Then: Should return 200 with Draw JSON
    sendDrawRequest(validJwtToken, validDrawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.id").value(validDrawId))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.title").isString())
        .andExpect(jsonPath("$.description").exists()) // Can be null
        .andExpect(jsonPath("$.state").exists())
        .andExpect(jsonPath("$.state").isString())
        .andExpect(jsonPath("$.drawDate").exists())
        .andExpect(jsonPath("$.drawDate").isString())
        .andExpect(jsonPath("$.participantCount").exists())
        .andExpect(jsonPath("$.participantCount").isNumber())
        .andExpect(jsonPath("$.maxParticipants").exists())
        .andExpect(jsonPath("$.maxParticipants").isNumber())
        .andExpect(jsonPath("$.creator").exists())
        .andExpect(jsonPath("$.creator.id").exists())
        .andExpect(jsonPath("$.creator.name").exists())
        .andExpect(jsonPath("$.participants").exists())
        .andExpect(jsonPath("$.participants").isArray())
        .andExpect(jsonPath("$.canJoin").exists())
        .andExpect(jsonPath("$.canJoin").isBoolean())
        .andExpect(jsonPath("$.canDraw").exists())
        .andExpect(jsonPath("$.canDraw").isBoolean())
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.createdAt").isString());
    // openedAt and archivedAt are optional and may not exist
  }

  @Test
  public void getDraw_withValidIdJoiningState_shouldReturnCorrectState() throws Exception {
    // Given: Valid JWT token and draw ID in JOINING state
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String joiningDrawId = "223e4567-e89b-12d3-a456-426614174001";

    // When: GET /api/v1/draws/{drawId} for draw in JOINING state
    // Then: Should return draw with correct state and capabilities
    sendDrawRequest(validJwtToken, joiningDrawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(joiningDrawId))
        .andExpect(jsonPath("$.state").value("JOINING"))
        .andExpect(jsonPath("$.canJoin").isBoolean())
        .andExpect(jsonPath("$.canDraw").value(false)) // Can't draw in JOINING state
        .andExpect(jsonPath("$.openedAt").doesNotExist()) // Not opened yet
        .andExpect(jsonPath("$.archivedAt").doesNotExist()); // Not archived yet
  }

  @Test
  public void getDraw_withValidIdOpenState_shouldReturnCorrectState() throws Exception {
    // Given: Valid JWT token and draw ID in OPEN state
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String openDrawId = "323e4567-e89b-12d3-a456-426614174002";

    // When: GET /api/v1/draws/{drawId} for draw in OPEN state
    // Then: Should return draw with correct state and capabilities
    sendDrawRequest(validJwtToken, openDrawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(openDrawId))
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.canJoin").value(false)) // Can't join in OPEN state
        .andExpect(jsonPath("$.canDraw").isBoolean()) // Depends on user participation
        .andExpect(jsonPath("$.openedAt").exists()) // Should have opened timestamp
        .andExpect(jsonPath("$.archivedAt").doesNotExist()); // Not archived yet
  }

  @Test
  public void getDraw_withValidIdArchivedState_shouldReturnCorrectState() throws Exception {
    // Given: Valid JWT token and draw ID in ARCHIVED state
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String archivedDrawId = "423e4567-e89b-12d3-a456-426614174003";

    // When: GET /api/v1/draws/{drawId} for draw in ARCHIVED state
    // Then: Should return draw with correct state and capabilities
    sendDrawRequest(validJwtToken, archivedDrawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(archivedDrawId))
        .andExpect(jsonPath("$.state").value("ARCHIVED"))
        .andExpect(jsonPath("$.canJoin").value(false)) // Can't join archived draw
        .andExpect(jsonPath("$.canDraw").value(false)) // Can't draw from archived draw
        .andExpect(jsonPath("$.openedAt").exists()) // Should have opened timestamp
        .andExpect(jsonPath("$.archivedAt").exists()); // Should have archived timestamp
  }

  @Test
  public void getDraw_withoutToken_shouldReturn401() throws Exception {
    // Given: Draw ID without authentication
    String validDrawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId} without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(get("/api/v1/draws/{drawId}", validDrawId))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDraw_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and draw ID
    String invalidJwtToken = "Bearer invalid.token.here";
    String validDrawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: GET /api/v1/draws/{drawId} with invalid token
    // Then: Should return 401 Unauthorized
    sendDrawRequest(invalidJwtToken, validDrawId)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDraw_withNonExistentDrawId_shouldReturn404() throws Exception {
    // Given: Valid JWT token but non-existent draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String nonExistentDrawId = "999e4567-e89b-12d3-a456-426614174999";

    // When: GET /api/v1/draws/{drawId} with non-existent ID
    // Then: Should return 404 Not Found
    sendDrawRequest(validJwtToken, nonExistentDrawId)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDraw_withInvalidUuidFormat_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String invalidUuid = "not-a-valid-uuid";

    // When: GET /api/v1/draws/{drawId} with invalid UUID format
    // Then: Should return 400 Bad Request
    sendDrawRequest(validJwtToken, invalidUuid)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDraw_withEmptyDrawId_shouldReturn404() throws Exception {
    // Given: Valid JWT token but empty draw ID
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/draws/ (empty draw ID)
    // Then: Should return 404 Not Found (different endpoint)
    mockMvc
        .perform(get("/api/v1/draws/").header("Authorization", validJwtToken))
        .andExpect(status().isNotFound());
  }

  @Test
  public void getDraw_withValidIdAndDescription_shouldReturnDescription() throws Exception {
    // Given: Valid JWT token and draw ID with description
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawWithDescriptionId = "523e4567-e89b-12d3-a456-426614174004";

    // When: GET /api/v1/draws/{drawId} for draw with description
    // Then: Should return draw with description field
    sendDrawRequest(validJwtToken, drawWithDescriptionId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(drawWithDescriptionId))
        .andExpect(jsonPath("$.description").exists())
        .andExpect(jsonPath("$.description").isString());
  }

  @Test
  public void getDraw_withValidIdNoDescription_shouldReturnNullDescription() throws Exception {
    // Given: Valid JWT token and draw ID without description
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String drawWithoutDescriptionId = "623e4567-e89b-12d3-a456-426614174005";

    // When: GET /api/v1/draws/{drawId} for draw without description
    // Then: Should return draw with null or missing description field
    sendDrawRequest(validJwtToken, drawWithoutDescriptionId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(drawWithoutDescriptionId))
        .andExpect(jsonPath("$.description").isEmpty()); // Can be null/empty
  }

  private ResultActions sendDrawRequest(String validJwtToken, String joiningDrawId)
      throws Exception {
    return mockMvc.perform(
        get("/api/v1/draws/{drawId}", joiningDrawId).header("Authorization", validJwtToken));
  }
}
