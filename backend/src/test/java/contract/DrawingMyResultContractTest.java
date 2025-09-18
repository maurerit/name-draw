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
  @Autowired private UserRepository userRepository;
  @MockBean private DrawService drawService;
  @MockBean private DrawingService drawingService;

  private User testUser;
  private User secondUser;

  private static final UUID DRAW_WITH_RESULT =
      UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final UUID NON_PARTICIPANT_DRAW =
      UUID.fromString("423e4567-e89b-12d3-a456-426614174003");
  private static final UUID NON_EXISTENT_DRAW =
      UUID.fromString("999e4567-e89b-12d3-a456-426614174999");
  private static final UUID PARTICIPANT_NO_RESULT_DRAW =
      UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
  private static final UUID WITH_PROFILE_DRAW =
      UUID.fromString("623e4567-e89b-12d3-a456-426614174005");

  @BeforeEach
  void setUp() {
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

    // existing draw where user is participant and has a result
    when(drawService.findDrawById(DRAW_WITH_RESULT))
        .thenReturn(Optional.of(buildDraw(DRAW_WITH_RESULT, DrawState.OPEN)));
    when(drawService.isUserParticipant(DRAW_WITH_RESULT, testUser)).thenReturn(true);
    when(drawingService.getMyDrawResult(DRAW_WITH_RESULT, testUser))
        .thenReturn(Optional.of(buildDrawnName(DRAW_WITH_RESULT, testUser, secondUser)));

    // non participant
    when(drawService.findDrawById(NON_PARTICIPANT_DRAW))
        .thenReturn(Optional.of(buildDraw(NON_PARTICIPANT_DRAW, DrawState.OPEN)));
    when(drawService.isUserParticipant(NON_PARTICIPANT_DRAW, testUser)).thenReturn(false);

    // non existent
    when(drawService.findDrawById(NON_EXISTENT_DRAW)).thenReturn(Optional.empty());

    // participant but no result yet
    when(drawService.findDrawById(PARTICIPANT_NO_RESULT_DRAW))
        .thenReturn(Optional.of(buildDraw(PARTICIPANT_NO_RESULT_DRAW, DrawState.OPEN)));
    when(drawService.isUserParticipant(PARTICIPANT_NO_RESULT_DRAW, testUser)).thenReturn(true);
    when(drawingService.getMyDrawResult(PARTICIPANT_NO_RESULT_DRAW, testUser))
        .thenReturn(Optional.empty());

    // with profile draw
    when(drawService.findDrawById(WITH_PROFILE_DRAW))
        .thenReturn(Optional.of(buildDraw(WITH_PROFILE_DRAW, DrawState.OPEN)));
    when(drawService.isUserParticipant(WITH_PROFILE_DRAW, testUser)).thenReturn(true);
    when(drawingService.getMyDrawResult(WITH_PROFILE_DRAW, testUser))
        .thenReturn(Optional.of(buildDrawnName(WITH_PROFILE_DRAW, testUser, secondUser)));
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
        .archivedAt(null)
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
  public void getMyDrawResult_withValidParticipantAndResult_shouldReturn200WithDrawResult()
      throws Exception {
    // Given: Valid JWT token for participant and draw ID where user has drawn
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawIdWithResult = DRAW_WITH_RESULT.toString();

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
    String validDrawId = DRAW_WITH_RESULT.toString();

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
    String validDrawId = DRAW_WITH_RESULT.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = NON_PARTICIPANT_DRAW.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String nonExistentDrawId = NON_EXISTENT_DRAW.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawIdNoResult = PARTICIPANT_NO_RESULT_DRAW.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawIdWithUserProfile = WITH_PROFILE_DRAW.toString();

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
