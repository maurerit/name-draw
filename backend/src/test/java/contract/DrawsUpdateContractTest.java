package contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.namedraw.model.Draw;
import com.namedraw.model.Draw.DrawState;
import com.namedraw.model.User;
import com.namedraw.repository.UserRepository;
import com.namedraw.service.DrawService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Contract test for PUT /draws/{drawId} endpoint
 *
 * <p>This test verifies the API contract for updating a draw. Expected to FAIL until DrawController
 * is implemented.
 *
 * <p>API Contract: - PUT /api/v1/draws/{drawId} - Requires Bearer authentication - Request body:
 * UpdateDrawRequest JSON - Returns 200 with Draw JSON on success - Returns 400 if request is
 * invalid - Returns 401 if unauthorized - Returns 403 if forbidden (not creator or wrong state) -
 * Returns 404 if draw not found - UpdateDrawRequest schema: {title?: string (1-100 chars),
 * description?: string (max 500 chars), drawDate?: date, maxParticipants?: integer (2-30)} - Draw
 * schema: {id: uuid, title: string, description?: string, state: enum, drawDate: date,
 * participantCount: number, maxParticipants: number, creator: User, participants: User[], canJoin:
 * boolean, canDraw: boolean, createdAt: datetime, openedAt?: datetime, archivedAt?: datetime}
 * Business Rules: Only creator can update, only in JOINING state
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawsUpdateContractTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @MockBean private DrawService drawService;

  @BeforeEach
  void setUp() {
    User testUser = ContractTestConfig.getTestUser();
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    UUID secondUserId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
    User secondUser =
        User.builder()
            .id(secondUserId)
            .oauthProvider("google")
            .oauthId("test456")
            .name("Second User")
            .email("second@example.com")
            .isActive(true)
            .build();
    when(userRepository.findById(secondUserId)).thenReturn(Optional.of(secondUser));

    // Existing JOINING draw owned by testUser
    UUID existingDrawId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    Draw joiningDraw =
        Draw.builder()
            .id(existingDrawId)
            .creator(testUser)
            .title("Family Christmas Draw")
            .description("Old desc")
            .state(DrawState.JOINING)
            .drawDate(LocalDate.of(2024, 12, 20))
            .participantCount(3)
            .maxParticipants(10)
            .createdAt(LocalDateTime.now().minusDays(5))
            .build();
    when(drawService.findDrawById(existingDrawId)).thenReturn(Optional.of(joiningDraw));
    when(drawService.updateDraw(
            org.mockito.ArgumentMatchers.eq(existingDrawId),
            org.mockito.ArgumentMatchers.eq(testUser),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            inv -> {
              String title = inv.getArgument(2);
              String description = inv.getArgument(3);
              LocalDate drawDate = inv.getArgument(4);
              Integer maxP = inv.getArgument(5);
              // Simulate service-level validation: at least one field must be provided
              if (title == null && description == null && drawDate == null && maxP == null) {
                throw new IllegalArgumentException("At least one field must be provided to update");
              }
              if (title != null) joiningDraw.setTitle(title);
              if (description != null) joiningDraw.setDescription(description);
              if (drawDate != null) joiningDraw.setDrawDate(drawDate);
              if (maxP != null) joiningDraw.setMaxParticipants(maxP);
              return joiningDraw;
            });

    // OPEN draw owned by testUser -> 403 on update
    UUID openDrawId = UUID.fromString("456e7890-e89b-12d3-a456-426614174001");
    Draw openDraw =
        Draw.builder()
            .id(openDrawId)
            .creator(testUser)
            .title("Open Draw")
            .state(DrawState.OPEN)
            .participantCount(3)
            .maxParticipants(10)
            .drawDate(LocalDate.of(2024, 12, 25))
            .openedAt(LocalDateTime.now().minusDays(1))
            .createdAt(LocalDateTime.now().minusDays(5))
            .build();
    when(drawService.findDrawById(openDrawId)).thenReturn(Optional.of(openDraw));

    // Draw owned by different user -> 403
    when(drawService.findDrawById(existingDrawId)).thenReturn(Optional.of(joiningDraw));
    // When called with different requester, controller forbids before calling service

    // Non-existent draw -> empty
    UUID nonExistent = UUID.fromString("999e9999-e99b-99d9-a999-999999999999");
    when(drawService.findDrawById(nonExistent)).thenReturn(Optional.empty());
  }

  @Test
  public void updateDraw_withValidRequest_shouldReturn200WithUpdatedDrawJson() throws Exception {
    // Given: Valid JWT token and update draw request for existing draw
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest =
        "{"
            + "\"title\": \"Updated Family Christmas Draw\","
            + "\"description\": \"Updated description for Christmas name drawing\","
            + "\"drawDate\": \"2024-12-20\","
            + "\"maxParticipants\": 15"
            + "}";

    // When: PUT /api/v1/draws/{drawId}
    // Then: Should return 200 with updated Draw JSON
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(drawId))
        .andExpect(jsonPath("$.title").value("Updated Family Christmas Draw"))
        .andExpect(
            jsonPath("$.description").value("Updated description for Christmas name drawing"))
        .andExpect(jsonPath("$.state").exists())
        .andExpect(jsonPath("$.drawDate").value("2024-12-20"))
        .andExpect(jsonPath("$.participantCount").exists())
        .andExpect(jsonPath("$.participantCount").isNumber())
        .andExpect(jsonPath("$.maxParticipants").value(15))
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
        .andExpect(jsonPath("$.openedAt").doesNotExist())
        .andExpect(jsonPath("$.archivedAt").doesNotExist());
  }

  @Test
  public void updateDraw_withPartialRequest_shouldReturn200WithUpdatedDrawJson() throws Exception {
    // Given: Valid JWT token and partial update (only title)
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest = "{" + "\"title\": \"New Title Only\"" + "}";

    // When: PUT /api/v1/draws/{drawId}
    // Then: Should return 200 with updated Draw JSON
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(drawId))
        .andExpect(jsonPath("$.title").value("New Title Only"));
  }

  @Test
  public void updateDraw_withoutToken_shouldReturn401() throws Exception {
    // Given: Update draw request without authentication
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest = "{" + "\"title\": \"Unauthorized Update\"" + "}";

    // When: PUT /api/v1/draws/{drawId} without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            put("/api/v1/draws/" + drawId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateDrawRequest))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and update draw request
    String invalidJwtToken = "Bearer invalid.token.here";
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest = "{" + "\"title\": \"Invalid Token Update\"" + "}";

    // When: PUT /api/v1/draws/{drawId} with invalid token
    // Then: Should return 401 Unauthorized
    executeDrawUpdate(invalidJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_asNonCreator_shouldReturn403() throws Exception {
    // Given: Valid JWT token but user is not the creator of the draw
    String validJwtToken =
        "Bearer " + TestTokenGenerator.generateValidAccessTokenForDifferentUser();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest = "{" + "\"title\": \"Non-Creator Update\"" + "}";

    // When: PUT /api/v1/draws/{drawId} by non-creator
    // Then: Should return 403 Forbidden
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_inWrongState_shouldReturn403() throws Exception {
    // Given: Valid JWT token but draw is not in JOINING state (e.g., OPEN or ARCHIVED)
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "456e7890-e89b-12d3-a456-426614174001"; // Draw in OPEN state
    String updateDrawRequest = "{" + "\"title\": \"Update Open Draw\"" + "}";

    // When: PUT /api/v1/draws/{drawId} for draw not in JOINING state
    // Then: Should return 403 Forbidden
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withNonexistentDrawId_shouldReturn404() throws Exception {
    // Given: Valid JWT token but draw does not exist
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String nonexistentDrawId = "999e9999-e99b-99d9-a999-999999999999";
    String updateDrawRequest = "{" + "\"title\": \"Update Nonexistent Draw\"" + "}";

    // When: PUT /api/v1/draws/{drawId} for nonexistent draw
    // Then: Should return 404 Not Found
    executeDrawUpdate(validJwtToken, nonexistentDrawId, updateDrawRequest)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withTitleTooLong_shouldReturn400() throws Exception {
    // Given: Valid JWT token but title exceeds 100 characters
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String longTitle = "A".repeat(101); // 101 characters, exceeds 100 char limit
    String updateDrawRequest = "{" + "\"title\": \"" + longTitle + "\"" + "}";

    // When: PUT /api/v1/draws/{drawId} with title too long
    // Then: Should return 400 Bad Request
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withDescriptionTooLong_shouldReturn400() throws Exception {
    // Given: Valid JWT token but description exceeds 500 characters
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String longDescription = "A".repeat(501); // 501 characters, exceeds 500 char limit
    String updateDrawRequest =
        "{" + "\"title\": \"Valid Title\"," + "\"description\": \"" + longDescription + "\"" + "}";

    // When: PUT /api/v1/draws/{drawId} with description too long
    // Then: Should return 400 Bad Request
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withMaxParticipantsTooLow_shouldReturn400() throws Exception {
    // Given: Valid JWT token but maxParticipants below minimum (2)
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest = "{" + "\"title\": \"Valid Title\"," + "\"maxParticipants\": 1" + "}";

    // When: PUT /api/v1/draws/{drawId} with maxParticipants too low
    // Then: Should return 400 Bad Request
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withMaxParticipantsTooHigh_shouldReturn400() throws Exception {
    // Given: Valid JWT token but maxParticipants above maximum (30)
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest =
        "{" + "\"title\": \"Valid Title\"," + "\"maxParticipants\": 31" + "}";

    // When: PUT /api/v1/draws/{drawId} with maxParticipants too high
    // Then: Should return 400 Bad Request
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withInvalidDateFormat_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid date format
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String updateDrawRequest =
        "{"
            + "\"title\": \"Valid Title\","
            + "\"drawDate\": \"12/15/2024\"" // Invalid format, should be YYYY-MM-DD
            + "}";

    // When: PUT /api/v1/draws/{drawId} with invalid date format
    // Then: Should return 400 Bad Request
    executeDrawUpdate(validJwtToken, drawId, updateDrawRequest)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withEmptyBody_shouldReturn400() throws Exception {
    // Given: Valid JWT token but empty request body
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";

    // When: PUT /api/v1/draws/{drawId} with empty body
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            put("/api/v1/draws/" + drawId)
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withMalformedJson_shouldReturn400() throws Exception {
    // Given: Valid JWT token but malformed JSON
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "123e4567-e89b-12d3-a456-426614174000";
    String malformedJson = "{\"title\": \"Test Draw\", \"drawDate\": "; // Missing closing

    // When: PUT /api/v1/draws/{drawId} with malformed JSON
    // Then: Should return 400 Bad Request
    executeDrawUpdate(validJwtToken, drawId, malformedJson)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void updateDraw_withInvalidUuidFormat_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format for drawId
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String invalidDrawId = "not-a-valid-uuid";
    String updateDrawRequest = "{" + "\"title\": \"Valid Title\"" + "}";

    // When: PUT /api/v1/draws/{drawId} with invalid UUID format
    // Then: Should return 400 Bad Request
    executeDrawUpdate(validJwtToken, invalidDrawId, updateDrawRequest)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  private ResultActions executeDrawUpdate(
      String validJwtToken, String drawId, String updateDrawRequest) throws Exception {
    return mockMvc.perform(
        put("/api/v1/draws/" + drawId)
            .header("Authorization", validJwtToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateDrawRequest));
  }
}
