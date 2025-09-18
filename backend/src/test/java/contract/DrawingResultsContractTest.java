package contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.namedraw.model.Draw;
import com.namedraw.model.Draw.DrawState;
import com.namedraw.model.DrawnName;
import com.namedraw.model.User;
import com.namedraw.repository.UserRepository;
import com.namedraw.service.DrawService;
import com.namedraw.service.DrawingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawingResultsContractTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @MockBean private DrawService drawService;
  @MockBean private DrawingService drawingService;

  private User testUser;
  private User secondUser;

  private static final UUID DRAW_WITH_RESULTS =
      UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final UUID DRAW_NO_RESULTS =
      UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
  private static final UUID ARCHIVED_DRAW = UUID.fromString("323e4567-e89b-12d3-a456-426614174002");
  private static final UUID ACTIVE_NON_PARTICIPANT_DRAW =
      UUID.fromString("423e4567-e89b-12d3-a456-426614174003");
  private static final UUID MULTIPLE_RESULTS_DRAW =
      UUID.fromString("523e4567-e89b-12d3-a456-426614174004");
  private static final UUID WITH_USER_PROFILE_DRAW =
      UUID.fromString("623e4567-e89b-12d3-a456-426614174005");
  private static final UUID NON_EXISTENT_DRAW =
      UUID.fromString("999e4567-e89b-12d3-a456-426614174999");

  @BeforeEach
  void setUp() {
    // users
    testUser = ContractTestConfig.getTestUser();
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    UUID secondUserId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
    secondUser =
        User.builder()
            .id(secondUserId)
            .oauthProvider("google")
            .oauthId("test456")
            .name("Second User")
            .email("second@example.com")
            .isActive(true)
            .build();
    when(userRepository.findById(secondUserId)).thenReturn(Optional.of(secondUser));

    // draw with results (participant)
    when(drawService.findDrawById(DRAW_WITH_RESULTS))
        .thenReturn(Optional.of(buildDraw(DRAW_WITH_RESULTS, DrawState.OPEN)));
    when(drawService.isUserParticipant(DRAW_WITH_RESULTS, testUser)).thenReturn(true);
    when(drawingService.getDrawResults(DRAW_WITH_RESULTS))
        .thenReturn(List.of(buildDrawnName(DRAW_WITH_RESULTS, testUser, secondUser)));

    // draw with no results (participant)
    when(drawService.findDrawById(DRAW_NO_RESULTS))
        .thenReturn(Optional.of(buildDraw(DRAW_NO_RESULTS, DrawState.OPEN)));
    when(drawService.isUserParticipant(DRAW_NO_RESULTS, testUser)).thenReturn(true);
    when(drawingService.getDrawResults(DRAW_NO_RESULTS)).thenReturn(List.of());

    // archived draw (non-participant should still access)
    when(drawService.findDrawById(ARCHIVED_DRAW))
        .thenReturn(Optional.of(buildDraw(ARCHIVED_DRAW, DrawState.ARCHIVED)));
    when(drawService.isUserParticipant(ARCHIVED_DRAW, testUser)).thenReturn(false);
    when(drawingService.getDrawResults(ARCHIVED_DRAW))
        .thenReturn(List.of(buildDrawnName(ARCHIVED_DRAW, testUser, secondUser)));

    // active draw non-participant -> 403
    when(drawService.findDrawById(ACTIVE_NON_PARTICIPANT_DRAW))
        .thenReturn(
            Optional.of(buildDraw(ACTIVE_NON_PARTICIPANT_DRAW, DrawState.OPEN, secondUser)));
    when(drawService.isUserParticipant(ACTIVE_NON_PARTICIPANT_DRAW, testUser)).thenReturn(false);

    // non existent
    when(drawService.findDrawById(NON_EXISTENT_DRAW)).thenReturn(Optional.empty());

    // multiple results
    when(drawService.findDrawById(MULTIPLE_RESULTS_DRAW))
        .thenReturn(Optional.of(buildDraw(MULTIPLE_RESULTS_DRAW, DrawState.OPEN)));
    when(drawService.isUserParticipant(MULTIPLE_RESULTS_DRAW, testUser)).thenReturn(true);
    when(drawingService.getDrawResults(MULTIPLE_RESULTS_DRAW))
        .thenReturn(
            List.of(
                buildDrawnName(MULTIPLE_RESULTS_DRAW, testUser, secondUser),
                buildDrawnName(MULTIPLE_RESULTS_DRAW, testUser, secondUser)));

    // with user profile
    when(drawService.findDrawById(WITH_USER_PROFILE_DRAW))
        .thenReturn(Optional.of(buildDraw(WITH_USER_PROFILE_DRAW, DrawState.OPEN)));
    when(drawService.isUserParticipant(WITH_USER_PROFILE_DRAW, testUser)).thenReturn(true);
    when(drawingService.getDrawResults(WITH_USER_PROFILE_DRAW))
        .thenReturn(List.of(buildDrawnName(WITH_USER_PROFILE_DRAW, testUser, secondUser)));
  }

  private Draw buildDraw(UUID id, DrawState state) {
    return Draw.builder()
        .id(id)
        .creator(testUser)
        .title("Test Draw")
        .state(state)
        .drawDate(LocalDate.now())
        .participantCount(2)
        .maxParticipants(10)
        .createdAt(LocalDateTime.now().minusDays(1))
        .openedAt(state == DrawState.OPEN ? LocalDateTime.now().minusHours(1) : null)
        .archivedAt(state == DrawState.ARCHIVED ? LocalDateTime.now().minusHours(1) : null)
        .build();
  }

  private Draw buildDraw(UUID id, DrawState state, User creator) {
    return Draw.builder()
        .id(id)
        .creator(creator)
        .title("Test Draw")
        .state(state)
        .drawDate(LocalDate.now())
        .participantCount(2)
        .maxParticipants(10)
        .createdAt(LocalDateTime.now().minusDays(1))
        .openedAt(state == DrawState.OPEN ? LocalDateTime.now().minusHours(1) : null)
        .archivedAt(state == DrawState.ARCHIVED ? LocalDateTime.now().minusHours(1) : null)
        .build();
  }

  private DrawnName buildDrawnName(UUID drawId, User drawer, User drawn) {
    return DrawnName.builder()
        .draw(buildDraw(drawId, DrawState.OPEN))
        .drawerUser(drawer)
        .drawnUser(drawn)
        .drawnAt(LocalDateTime.now())
        .build();
  }

  @Test
  public void getDrawResults_withValidIdAndParticipant_shouldReturn200WithResultsArray()
      throws Exception {
    // Given: Valid JWT token for participant and draw ID with results
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawIdWithResults = DRAW_WITH_RESULTS.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawIdNoResults = DRAW_NO_RESULTS.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String archivedDrawId = ARCHIVED_DRAW.toString();

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
    String validDrawId = DRAW_WITH_RESULTS.toString();

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
    String validDrawId = DRAW_WITH_RESULTS.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String activeDrawId = ACTIVE_NON_PARTICIPANT_DRAW.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String nonExistentDrawId = NON_EXISTENT_DRAW.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawIdMultipleResults = MULTIPLE_RESULTS_DRAW.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawIdWithUserProfile = WITH_USER_PROFILE_DRAW.toString();

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

  private ResultActions retrieveDrawResults(String validJwtToken, String drawIdWithResults)
      throws Exception {
    return mockMvc.perform(
        get("/api/v1/draws/{drawId}/results", drawIdWithResults)
            .header("Authorization", validJwtToken));
  }
}
