package contract;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.namedraw.model.Draw;
import com.namedraw.model.Draw.DrawState;
import com.namedraw.model.Participation;
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
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawsOpenContractTest {

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

    // Existing draw where user is creator and can open (>=2 participants, JOINING)
    UUID validDrawId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    Draw creatorJoinable = buildDraw(validDrawId, testUser, DrawState.JOINING, 2, 10, null, null);
    // mark current user as participant so canDraw=true after opening
    creatorJoinable
        .getParticipations()
        .add(
            Participation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .draw(creatorJoinable)
                .build());
    when(drawService.findDrawById(validDrawId)).thenReturn(Optional.of(creatorJoinable));
    when(drawService.openDrawForDrawing(validDrawId, testUser))
        .thenAnswer(
            inv -> {
              creatorJoinable.setState(DrawState.OPEN);
              creatorJoinable.setOpenedAt(LocalDateTime.now());
              return creatorJoinable;
            });

    // Draw created by different user -> 403
    UUID notCreatorId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    Draw notCreatorsDraw =
        buildDraw(notCreatorId, secondUser, DrawState.JOINING, 2, 10, null, null);
    when(drawService.findDrawById(notCreatorId)).thenReturn(Optional.of(notCreatorsDraw));

    // Draw already OPEN -> controller checks state? Service should throw IllegalStateException
    UUID alreadyOpenId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    Draw alreadyOpen =
        buildDraw(
            alreadyOpenId, testUser, DrawState.OPEN, 2, 10, LocalDateTime.now().minusDays(1), null);
    when(drawService.findDrawById(alreadyOpenId)).thenReturn(Optional.of(alreadyOpen));
    when(drawService.openDrawForDrawing(alreadyOpenId, testUser))
        .thenThrow(new IllegalStateException("Draw cannot be opened - not in JOINING state"));

    // Draw with insufficient participants (<2) -> IllegalStateException -> 400
    UUID insufficientId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    Draw insufficient = buildDraw(insufficientId, testUser, DrawState.JOINING, 1, 10, null, null);
    when(drawService.findDrawById(insufficientId)).thenReturn(Optional.of(insufficient));
    when(drawService.openDrawForDrawing(insufficientId, testUser))
        .thenThrow(
            new IllegalStateException("Draw cannot be opened - requires at least 2 participants"));

    // Non-existent draw returns empty
    UUID nonExistent = UUID.fromString("999e8400-e29b-41d4-a716-446655440999");
    when(drawService.findDrawById(nonExistent)).thenReturn(Optional.empty());
  }

  private Draw buildDraw(
      UUID id,
      User creator,
      DrawState state,
      int participantCount,
      int maxParticipants,
      LocalDateTime openedAt,
      LocalDateTime archivedAt) {
    Draw d =
        Draw.builder()
            .id(id)
            .creator(creator)
            .title("Test Draw")
            .description(null)
            .state(state)
            .drawDate(LocalDate.now())
            .participantCount(participantCount)
            .maxParticipants(maxParticipants)
            .createdAt(LocalDateTime.now().minusDays(2))
            .openedAt(openedAt)
            .archivedAt(archivedAt)
            .build();
    return d;
  }

  @Test
  public void openDraw_withValidCreatorRequest_shouldReturn200WithDrawJson() throws Exception {
    // Given: Valid JWT token and existing draw UUID where user is creator
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
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
    return mockMvc.perform(
        post("/api/v1/draws/{drawId}/open", drawId)
            .header("Authorization", validJwtToken)
            .contentType(MediaType.APPLICATION_JSON));
  }
}
