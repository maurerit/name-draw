package contract;

import static org.mockito.ArgumentMatchers.*;
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
 * Contract test for POST /draws/{drawId}/join endpoint
 *
 * <p>This test verifies the API contract for joining a draw as a participant. Expected to FAIL
 * until DrawController is implemented.
 *
 * <p>API Contract: - POST /api/v1/draws/{drawId}/join - Requires Bearer authentication - Path
 * parameter: drawId (UUID) - Returns 200 with Participation JSON on success - Returns 400 if cannot
 * join (wrong state, already joined, etc.) - Returns 401 if unauthorized - Returns 404 if draw not
 * found - Participation schema: {id: uuid, draw: Draw, joinedAt: datetime}
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, ContractTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawsJoinContractTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @MockBean private DrawService drawService;

  private User testUser;
  private User secondUser;

  @BeforeEach
  void setUp() {
    // Primary authenticated user
    testUser = ContractTestConfig.getTestUser();
    when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

    // A second active user to act as creator for non-creator scenarios
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

    // Common dates
    LocalDate defaultDate = LocalDate.now().plusDays(7);

    // Draw IDs used in tests
    UUID validJoinId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    UUID alreadyJoinedId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    UUID notJoiningId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
    UUID atCapacityId = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
    UUID creatorDrawId = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
    UUID nonExistentId = UUID.fromString("999e8400-e29b-41d4-a716-446655440999");

    // Valid join scenario: JOINING state, created by secondUser
    Draw validJoinDraw =
        Draw.builder()
            .id(validJoinId)
            .creator(secondUser)
            .title("A Joinable Draw")
            .description(null)
            .state(DrawState.JOINING)
            .drawDate(defaultDate)
            .participantCount(1)
            .maxParticipants(10)
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();
    when(drawService.findDrawById(validJoinId)).thenReturn(Optional.of(validJoinDraw));
    when(drawService.joinDraw(eq(validJoinId), eq(testUser)))
        .thenAnswer(
            invocation ->
                Participation.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .draw(validJoinDraw)
                    .joinedAt(LocalDateTime.now())
                    .build());

    // Already joined scenario: throw IllegalArgumentException
    Draw alreadyJoinedDraw =
        Draw.builder()
            .id(alreadyJoinedId)
            .creator(secondUser)
            .title("Already Joined Draw")
            .description(null)
            .state(DrawState.JOINING)
            .drawDate(defaultDate)
            .participantCount(3)
            .maxParticipants(10)
            .createdAt(LocalDateTime.now().minusDays(2))
            .build();
    when(drawService.findDrawById(alreadyJoinedId)).thenReturn(Optional.of(alreadyJoinedDraw));
    when(drawService.joinDraw(eq(alreadyJoinedId), any(User.class)))
        .thenThrow(new IllegalArgumentException("User is already a participant in this draw"));

    // Not in JOINING state (e.g., OPEN)
    Draw notJoiningDraw =
        Draw.builder()
            .id(notJoiningId)
            .creator(secondUser)
            .title("Open Draw")
            .state(DrawState.OPEN)
            .drawDate(defaultDate)
            .participantCount(3)
            .maxParticipants(10)
            .createdAt(LocalDateTime.now().minusDays(3))
            .build();
    when(drawService.findDrawById(notJoiningId)).thenReturn(Optional.of(notJoiningDraw));
    when(drawService.joinDraw(eq(notJoiningId), any(User.class)))
        .thenThrow(new IllegalStateException("Cannot join draw - draw is not in JOINING state"));

    // At capacity
    Draw atCapacityDraw =
        Draw.builder()
            .id(atCapacityId)
            .creator(secondUser)
            .title("At Capacity")
            .state(DrawState.JOINING)
            .drawDate(defaultDate)
            .participantCount(10)
            .maxParticipants(10)
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();
    when(drawService.findDrawById(atCapacityId)).thenReturn(Optional.of(atCapacityDraw));
    when(drawService.joinDraw(eq(atCapacityId), any(User.class)))
        .thenThrow(new IllegalStateException("Cannot join draw - maximum capacity reached"));

    // Creator attempting to join own draw
    Draw creatorDraw =
        Draw.builder()
            .id(creatorDrawId)
            .creator(testUser)
            .title("My Own Draw")
            .state(DrawState.JOINING)
            .drawDate(defaultDate)
            .participantCount(0)
            .maxParticipants(10)
            .createdAt(LocalDateTime.now().minusDays(1))
            .build();
    when(drawService.findDrawById(creatorDrawId)).thenReturn(Optional.of(creatorDraw));
    when(drawService.joinDraw(eq(creatorDrawId), eq(testUser)))
        .thenThrow(new IllegalArgumentException("Creator cannot join own draw"));

    // Non-existent draw
    when(drawService.findDrawById(nonExistentId)).thenReturn(Optional.empty());
  }

  @Test
  public void joinDraw_withValidRequest_shouldReturn200WithParticipationJson() throws Exception {
    // Given: Valid JWT token and existing draw UUID
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/join
    // Then: Should return 200 with Participation JSON
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.draw").exists())
        .andExpect(jsonPath("$.draw.id").value(drawId))
        .andExpect(jsonPath("$.draw.title").exists())
        .andExpect(jsonPath("$.draw.state").exists())
        .andExpect(jsonPath("$.draw.participantCount").exists())
        .andExpect(jsonPath("$.draw.canJoin").exists())
        .andExpect(jsonPath("$.draw.canDraw").exists())
        .andExpect(jsonPath("$.joinedAt").exists())
        .andExpect(jsonPath("$.joinedAt").isString());
  }

  @Test
  public void joinDraw_withoutToken_shouldReturn401() throws Exception {
    // Given: Draw ID without authentication
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/join without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            post("/api/v1/draws/{drawId}/join", drawId).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and draw ID
    String invalidJwtToken = "Bearer invalid.token.here";
    String drawId = "550e8400-e29b-41d4-a716-446655440000";

    // When: POST /api/v1/draws/{drawId}/join with invalid token
    // Then: Should return 401 Unauthorized
    joinDrawWithToken(invalidJwtToken, drawId)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_withNonExistentDraw_shouldReturn404() throws Exception {
    // Given: Valid JWT token but non-existent draw ID
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String nonExistentDrawId = "999e8400-e29b-41d4-a716-446655440999";

    // When: POST /api/v1/draws/{drawId}/join with non-existent draw
    // Then: Should return 404 Not Found
    joinDrawWithToken(validJwtToken, nonExistentDrawId)
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenAlreadyJoined_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw where user is already a participant
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "550e8400-e29b-41d4-a716-446655440001"; // Draw user already joined

    // When: POST /api/v1/draws/{drawId}/join when already joined
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenDrawNotInJoiningState_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw that is not in JOINING state (e.g., OPEN or ARCHIVED)
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "550e8400-e29b-41d4-a716-446655440002"; // Draw in OPEN state

    // When: POST /api/v1/draws/{drawId}/join when draw is not in JOINING state
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenDrawAtMaxCapacity_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw that has reached maximum participants
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "550e8400-e29b-41d4-a716-446655440003"; // Draw at max capacity

    // When: POST /api/v1/draws/{drawId}/join when draw is at max capacity
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_whenUserIsCreator_shouldReturn400() throws Exception {
    // Given: Valid JWT token and draw where the user is the creator
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String drawId = "550e8400-e29b-41d4-a716-446655440004"; // Draw created by this user

    // When: POST /api/v1/draws/{drawId}/join when user is the creator
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, drawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void joinDraw_withInvalidDrawId_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid UUID format for drawId
    String validJwtToken = "Bearer " + TestTokenGenerator.generateValidAccessToken();
    String invalidDrawId = "not-a-valid-uuid";

    // When: POST /api/v1/draws/{drawId}/join with invalid UUID
    // Then: Should return 400 Bad Request
    joinDrawWithToken(validJwtToken, invalidDrawId)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  private ResultActions joinDrawWithToken(String validJwtToken, String drawId) throws Exception {
    return mockMvc.perform(
        post("/api/v1/draws/{drawId}/join", drawId)
            .header("Authorization", validJwtToken)
            .contentType(MediaType.APPLICATION_JSON));
  }
}
