package contract;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawingPerformContractTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @MockBean private DrawService drawService;
  @MockBean private DrawingService drawingService;

  private User testUser;
  private User secondUser;

  private static final UUID VALID_DRAW_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
  private static final UUID NOT_PARTICIPANT_DRAW_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
  private static final UUID NOT_OPEN_DRAW_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
  private static final UUID ALREADY_DREW_DRAW_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
  private static final UUID IN_PROGRESS_DRAW_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
  private static final UUID NO_NAMES_LEFT_DRAW_ID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440005");
  private static final UUID NON_EXISTENT_DRAW_ID =
      UUID.fromString("999e8400-e29b-41d4-a716-446655440999");

  @BeforeEach
  void setUp() {
    // Users
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

    // Valid draw for success path
    when(drawService.findDrawById(VALID_DRAW_ID))
        .thenReturn(Optional.of(buildDraw(VALID_DRAW_ID, DrawState.OPEN)));
    when(drawService.isUserParticipant(VALID_DRAW_ID, testUser)).thenReturn(true);
    when(drawingService.performDraw(eq(VALID_DRAW_ID), eq(testUser)))
        .thenAnswer(inv -> buildDrawnName(VALID_DRAW_ID, testUser, secondUser));

    // Non-existent
    when(drawService.findDrawById(NON_EXISTENT_DRAW_ID)).thenReturn(Optional.empty());

    // Not participant
    when(drawService.findDrawById(NOT_PARTICIPANT_DRAW_ID))
        .thenReturn(Optional.of(buildDraw(NOT_PARTICIPANT_DRAW_ID, DrawState.OPEN)));
    when(drawService.isUserParticipant(NOT_PARTICIPANT_DRAW_ID, testUser)).thenReturn(false);

    // Not open state -> 400
    when(drawService.findDrawById(NOT_OPEN_DRAW_ID))
        .thenReturn(Optional.of(buildDraw(NOT_OPEN_DRAW_ID, DrawState.JOINING)));
    when(drawService.isUserParticipant(NOT_OPEN_DRAW_ID, testUser)).thenReturn(true);
    when(drawingService.performDraw(eq(NOT_OPEN_DRAW_ID), eq(testUser)))
        .thenThrow(new IllegalStateException("Draw is not open for drawing"));

    // Already drew -> 400
    when(drawService.findDrawById(ALREADY_DREW_DRAW_ID))
        .thenReturn(Optional.of(buildDraw(ALREADY_DREW_DRAW_ID, DrawState.OPEN)));
    when(drawService.isUserParticipant(ALREADY_DREW_DRAW_ID, testUser)).thenReturn(true);
    when(drawingService.performDraw(eq(ALREADY_DREW_DRAW_ID), eq(testUser)))
        .thenThrow(new IllegalArgumentException("User has already drawn from this draw"));

    // In progress -> 409
    when(drawService.findDrawById(IN_PROGRESS_DRAW_ID))
        .thenReturn(Optional.of(buildDraw(IN_PROGRESS_DRAW_ID, DrawState.OPEN)));
    when(drawService.isUserParticipant(IN_PROGRESS_DRAW_ID, testUser)).thenReturn(true);
    when(drawingService.performDraw(eq(IN_PROGRESS_DRAW_ID), eq(testUser)))
        .thenThrow(new RuntimeException("Another draw is already in progress. Please try again."));

    // No names left -> 400
    when(drawService.findDrawById(NO_NAMES_LEFT_DRAW_ID))
        .thenReturn(Optional.of(buildDraw(NO_NAMES_LEFT_DRAW_ID, DrawState.OPEN)));
    when(drawService.isUserParticipant(NO_NAMES_LEFT_DRAW_ID, testUser)).thenReturn(true);
    when(drawingService.performDraw(eq(NO_NAMES_LEFT_DRAW_ID), eq(testUser)))
        .thenThrow(new RuntimeException("No eligible participants available to draw"));
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
  public void performDraw_withValidRequest_shouldReturn200WithDrawResultJson() throws Exception {
    // Given: Valid JWT token and existing draw UUID in OPEN state with participants
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = VALID_DRAW_ID.toString();

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
    String drawId = VALID_DRAW_ID.toString();

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
    String drawId = VALID_DRAW_ID.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String nonExistentDrawId = NON_EXISTENT_DRAW_ID.toString();

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = NOT_PARTICIPANT_DRAW_ID.toString(); // Draw user is not participant of

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = NOT_OPEN_DRAW_ID.toString(); // Draw in JOINING state

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = ALREADY_DREW_DRAW_ID.toString(); // Draw where user already drew

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = IN_PROGRESS_DRAW_ID.toString(); // Draw with concurrent access

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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = NO_NAMES_LEFT_DRAW_ID.toString(); // Draw with no names left

    // When: POST /api/v1/draws/{drawId}/draw when no names are left to draw
    // Then: Should return 400 Bad Request
    sendDrawRequest(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  private ResultActions sendDrawRequest(String validJwtToken, String drawId) throws Exception {
    return mockMvc.perform(
        post("/api/v1/draws/{drawId}/draw", drawId)
            .header("Authorization", validJwtToken)
            .contentType(MediaType.APPLICATION_JSON));
  }
}
