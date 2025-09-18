package integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.namedraw.model.User;
import com.namedraw.repository.UserRepository;
import contract.TestTokenGenerator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for the complete draw creator flow
 *
 * <p>This test verifies the end-to-end flow for a user creating and managing a draw. Expected to
 * FAIL until all components (controllers, services, repositories) are implemented.
 *
 * <p>Flow tested: 1. User authenticates via OAuth (mocked) 2. User creates a new draw 3. Other
 * participants join the draw (simulated) 4. Creator opens the draw for drawing 5. Creator monitors
 * participant drawing activity 6. Creator views final results when all participants have drawn
 *
 * <p>Business rules verified: - Draw creator can see all participants who have joined - Draw
 * creator can transition draw from JOINING to OPEN state - Draw creator can view drawing progress
 * and final results - Draw automatically archives after draw date - System prevents self-draws and
 * handles edge cases
 */
@SpringBootTest(classes = {com.namedraw.NameDrawApplication.class, IntegrationTestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class DrawCreatorFlowTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String creatorJwtToken;
  private String participant1JwtToken;
  private String participant2JwtToken;
  @Autowired private UserRepository userRepository;

  private UUID creatorId;
  private UUID participant1Id;
  private UUID participant2Id;

  @BeforeEach
  public void setUp() {
    // Ensure users exist in the database for the subjects we'll generate tokens for
    creatorId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    participant1Id = UUID.fromString("22222222-2222-2222-2222-222222222222");
    participant2Id = UUID.fromString("33333333-3333-3333-3333-333333333333");

    ensureUser(creatorId, "google", "google_123", "John Creator", "creator@example.com");
    ensureUser(
        participant1Id, "facebook", "facebook_456", "Alice Participant", "alice@example.com");
    ensureUser(participant2Id, "google", "google_789", "Bob Participant", "bob@example.com");

    // Generate signed JWTs using the same secret configured in application-test.properties
    creatorJwtToken =
        "Bearer " + TestTokenGenerator.generateValidAccessTokenForUser(creatorId, "John Creator");
    participant1JwtToken =
        "Bearer "
            + TestTokenGenerator.generateValidAccessTokenForUser(
                participant1Id, "Alice Participant");
    participant2JwtToken =
        "Bearer "
            + TestTokenGenerator.generateValidAccessTokenForUser(participant2Id, "Bob Participant");
  }

  private void ensureUser(UUID id, String provider, String oauthId, String name, String email) {
    userRepository
        .findById(id)
        .orElseGet(
            () ->
                userRepository.save(
                    User.builder()
                        .id(id)
                        .oauthProvider(provider)
                        .oauthId(oauthId + "_it")
                        .name(name)
                        .email(email)
                        .isActive(true)
                        .build()));
  }

  private String futureDate() {
    return java.time.LocalDate.now().plusDays(30).toString();
  }

  @Test
  public void drawCreatorFlow_completeScenario_shouldSucceed() throws Exception {
    // Step 1: Creator creates a new draw
    String createDrawRequest =
        "{"
            + "\"title\": \"Family Christmas Draw\","
            + "\"description\": \"Annual family Christmas name drawing\","
            + "\"drawDate\": \""
            + futureDate()
            + "\","
            + "\"maxParticipants\": 10"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/draws")
                    .header("Authorization", creatorJwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createDrawRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Family Christmas Draw"))
            .andExpect(jsonPath("$.state").value("JOINING"))
            .andExpect(jsonPath("$.participantCount").value(0))
            .andExpect(jsonPath("$.canJoin").value(true))
            .andExpect(jsonPath("$.canDraw").value(false))
            .andReturn();

    // Extract draw ID for subsequent operations
    String createResponseJson = createResult.getResponse().getContentAsString();
    JsonNode createResponseNode = objectMapper.readTree(createResponseJson);
    String drawId = createResponseNode.get("id").asText();

    // Step 2: Creator views empty participant list
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantCount").value(0))
        .andExpect(jsonPath("$.participants").isEmpty())
        .andExpect(jsonPath("$.state").value("JOINING"));

    // Step 3: Participants join the draw
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Step 4: Creator views updated participant list
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantCount").value(2))
        .andExpect(jsonPath("$.participants").isArray())
        .andExpect(jsonPath("$.participants", hasSize(2)))
        .andExpect(jsonPath("$.state").value("JOINING"));

    // Step 5: Creator opens the draw for drawing
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.canJoin").value(false))
        // creator cannot draw; only participants can
        .andExpect(jsonPath("$.canDraw").value(false));

    // Step 6: Verify draw state changed to OPEN
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.canJoin").value(false))
        .andExpect(jsonPath("$.canDraw").value(false))
        .andExpect(jsonPath("$.openedAt").exists());

    // Step 7: Participants draw names
    MvcResult draw1Result =
        mockMvc
            .perform(
                post("/api/v1/draws/" + drawId + "/draw")
                    .header("Authorization", participant1JwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drawnUser").exists())
            .andExpect(jsonPath("$.drawnAt").exists())
            .andReturn();

    MvcResult draw2Result =
        mockMvc
            .perform(
                post("/api/v1/draws/" + drawId + "/draw")
                    .header("Authorization", participant2JwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drawnUser").exists())
            .andExpect(jsonPath("$.drawnAt").exists())
            .andReturn();

    // Step 8: Verify no self-draws occurred
    String draw1ResponseJson = draw1Result.getResponse().getContentAsString();
    JsonNode draw1ResponseNode = objectMapper.readTree(draw1ResponseJson);
    String drawnUser1Id = draw1ResponseNode.get("drawnUser").get("id").asText();

    String draw2ResponseJson = draw2Result.getResponse().getContentAsString();
    JsonNode draw2ResponseNode = objectMapper.readTree(draw2ResponseJson);
    String drawnUser2Id = draw2ResponseNode.get("drawnUser").get("id").asText();

    // Verify participant 1 didn't draw themselves (would be participant-1-id)
    assertNotEquals(
        participant1Id.toString(), drawnUser1Id, "Participant 1 should not draw themselves");

    // Verify participant 2 didn't draw themselves (would be participant-2-id)
    assertNotEquals(
        participant2Id.toString(), drawnUser2Id, "Participant 2 should not draw themselves");

    // Step 9: Participant views complete drawing results (only participants are authorized until
    // archived)
    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/results")
                .header("Authorization", participant1JwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].drawnUser").exists())
        .andExpect(jsonPath("$[*].drawnAt").exists());

    // Step 10: Verify participants can view their drawn names later
    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/my-result")
                .header("Authorization", participant1JwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.drawnUser").exists())
        .andExpect(jsonPath("$.drawnAt").exists());

    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/my-result")
                .header("Authorization", participant2JwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.drawnUser").exists())
        .andExpect(jsonPath("$.drawnAt").exists());

    // Step 11: Verify participants cannot draw again
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participant1JwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("User has already drawn from this draw"));

    // Step 12: Verify no one else can join the open draw
    UUID newParticipantId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    ensureUser(newParticipantId, "facebook", "facebook_321", "New User", "new@example.com");
    String newParticipantToken =
        "Bearer "
            + TestTokenGenerator.generateValidAccessTokenForUser(newParticipantId, "New User");

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", newParticipantToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Cannot join draw - draw is not in JOINING state"));
  }

  @Test
  public void drawCreatorFlow_singleParticipant_shouldHandleEdgeCase() throws Exception {
    // Create a draw with only one participant to test edge case handling
    String createDrawRequest =
        "{"
            + "\"title\": \"Single Person Draw\","
            + "\"description\": \"Edge case test\","
            + "\"drawDate\": \""
            + futureDate()
            + "\","
            + "\"maxParticipants\": 10"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/draws")
                    .header("Authorization", creatorJwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createDrawRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String createResponseJson = createResult.getResponse().getContentAsString();
    JsonNode createResponseNode = objectMapper.readTree(createResponseJson);
    String drawId = createResponseNode.get("id").asText();

    // Only the creator joins (single participant)
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/join").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk());

    // Attempt to open draw with single participant
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(
            jsonPath("$.message")
                .value("Draw cannot be opened - requires at least 2 participants"));
  }

  @Test
  public void drawCreatorFlow_maxParticipants_shouldAutoOpen() throws Exception {
    // Create a draw with max participants set to 2 for testing
    String createDrawRequest =
        "{"
            + "\"title\": \"Auto Open Draw\","
            + "\"description\": \"Test auto-opening at max capacity\","
            + "\"drawDate\": \""
            + futureDate()
            + "\","
            + "\"maxParticipants\": 2"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/draws")
                    .header("Authorization", creatorJwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createDrawRequest))
            .andExpect(status().isCreated())
            .andReturn();

    String createResponseJson = createResult.getResponse().getContentAsString();
    JsonNode createResponseNode = objectMapper.readTree(createResponseJson);
    String drawId = createResponseNode.get("id").asText();

    // First participant joins
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    // Verify still in JOINING state
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("JOINING"));

    // Second participant joins - should auto-open
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Verify draw automatically transitioned to OPEN
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.participantCount").value(2))
        .andExpect(jsonPath("$.canJoin").value(false))
        .andExpect(jsonPath("$.canDraw").value(false));
  }
}
