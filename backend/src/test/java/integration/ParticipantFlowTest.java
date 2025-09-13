package integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for the complete participant flow
 *
 * <p>This test verifies the end-to-end flow for a user participating in a draw created by someone
 * else. Expected to FAIL until all components (controllers, services, repositories) are
 * implemented.
 *
 * <p>Flow tested: 1. Participant authenticates via OAuth (mocked) 2. Participant accesses draw via
 * shared link 3. Participant joins the draw 4. Participant waits for creator to open draw 5.
 * Participant draws a name when draw is opened 6. Participant views their drawn result 7.
 * Participant returns later to view result again
 *
 * <p>Business rules verified: - Participants can join draws in JOINING state - Participants cannot
 * join draws in OPEN, COMPLETED, or ARCHIVED states - Participants can only draw when draw is in
 * OPEN state - Participants can only draw once per draw - Participants can view their own result
 * but not others' results - System prevents self-draws when possible
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class ParticipantFlowTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String creatorJwtToken;
  private String participantJwtToken;
  private String participant2JwtToken;
  private String participant3JwtToken;

  @BeforeEach
  public void setUp() {
    // Mock JWT tokens for different users
    // In real implementation, these would be generated through OAuth flow
    creatorJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjcmVhdG9yLXVzZXItaWQiLCJuYW1lIjoiSm9obiBDcmVhdG9yIiwiaWF0IjoxNTE2MjM5MDIyfQ.creator-jwt-token";
    participantJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXJ0aWNpcGFudC1pZCIsIm5hbWUiOiJBbGljZSBQYXJ0aWNpcGFudCIsImlhdCI6MTUxNjIzOTAyMn0.participant-jwt-token";
    participant2JwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXJ0aWNpcGFudC0yLWlkIiwibmFtZSI6IkJvYiBQYXJ0aWNpcGFudCIsImlhdCI6MTUxNjIzOTAyMn0.participant2-jwt-token";
    participant3JwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXJ0aWNpcGFudC0zLWlkIiwibmFtZSI6IkNoYXJsaWUgUGFydGljaXBhbnQiLCJpYXQiOjE1MTYyMzkwMjJ9.participant3-jwt-token";
  }

  @Test
  public void participantFlow_completeScenario_shouldSucceed() throws Exception {
    // Step 1: Creator creates a draw (setup for participant flow)
    String createDrawRequest =
        "{"
            + "\"title\": \"Family Birthday Draw\","
            + "\"description\": \"Annual family birthday gift exchange\","
            + "\"drawDate\": \"2024-12-20\","
            + "\"maxParticipants\": 15"
            + "}";

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/draws")
                    .header("Authorization", creatorJwtToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createDrawRequest))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.state").value("JOINING"))
            .andReturn();

    // Extract draw ID for participant operations
    String createResponseJson = createResult.getResponse().getContentAsString();
    JsonNode createResponseNode = objectMapper.readTree(createResponseJson);
    String drawId = createResponseNode.get("id").asText();

    // Step 2: Participant accesses draw via shared link (GET draw details)
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", participantJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Family Birthday Draw"))
        .andExpect(jsonPath("$.description").value("Annual family birthday gift exchange"))
        .andExpect(jsonPath("$.state").value("JOINING"))
        .andExpect(jsonPath("$.canJoin").value(true))
        .andExpect(jsonPath("$.canDraw").value(false))
        .andExpect(jsonPath("$.participantCount").value(0));

    // Step 3: Participant joins the draw
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participantJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Successfully joined draw"));

    // Step 4: Verify participant can see themselves in the draw
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", participantJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.participantCount").value(1))
        .andExpect(jsonPath("$.participants").isArray())
        .andExpect(jsonPath("$.participants", hasSize(1)))
        .andExpect(jsonPath("$.participants[0].name").value("Alice Participant"));

    // Step 5: Additional participants join for proper draw functionality
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant3JwtToken))
        .andExpect(status().isOk());

    // Step 6: Participant tries to draw before draw is opened (should fail)
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participantJwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Draw is not open for drawing"));

    // Step 7: Creator opens the draw for drawing
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Draw opened for drawing"));

    // Step 8: Participant sees draw is now open
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", participantJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.canJoin").value(false))
        .andExpect(jsonPath("$.canDraw").value(true))
        .andExpect(jsonPath("$.openedAt").exists());

    // Step 9: Participant draws a name
    MvcResult drawResult =
        mockMvc
            .perform(
                post("/api/v1/draws/" + drawId + "/draw")
                    .header("Authorization", participantJwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drawnUser").exists())
            .andExpect(jsonPath("$.drawnUser.id").exists())
            .andExpect(jsonPath("$.drawnUser.name").exists())
            .andExpect(jsonPath("$.drawnAt").exists())
            .andReturn();

    // Step 10: Verify participant didn't draw themselves
    String drawResponseJson = drawResult.getResponse().getContentAsString();
    JsonNode drawResponseNode = objectMapper.readTree(drawResponseJson);
    String drawnUserId = drawResponseNode.get("drawnUser").get("id").asText();
    assertNotEquals("participant-id", drawnUserId, "Participant should not draw themselves");

    // Step 11: Participant views their drawn result
    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/my-result")
                .header("Authorization", participantJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.drawnUser").exists())
        .andExpect(jsonPath("$.drawnUser.id").value(drawnUserId))
        .andExpect(jsonPath("$.drawnAt").exists());

    // Step 12: Participant tries to draw again (should fail)
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participantJwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("User has already drawn a name in this draw"));

    // Step 13: Participant cannot see other participants' results
    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/results")
                .header("Authorization", participantJwtToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("Only draw creator can view all results"));

    // Step 14: Participant returns later and can still see their result
    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/my-result")
                .header("Authorization", participantJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.drawnUser.id").value(drawnUserId))
        .andExpect(jsonPath("$.drawnAt").exists());

    // Step 15: Participant can see their participation in their draws list
    mockMvc
        .perform(get("/api/v1/users/me/draws").header("Authorization", participantJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
        .andExpect(jsonPath("$[?(@.id == '" + drawId + "')]").exists());
  }

  @Test
  public void participantFlow_cannotJoinClosedDraw_shouldFail() throws Exception {
    // Creator creates and immediately opens a draw
    String createDrawRequest =
        "{"
            + "\"title\": \"Closed Draw Test\","
            + "\"description\": \"Test joining a closed draw\","
            + "\"drawDate\": \"2024-12-20\","
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

    // Add minimum participants and open the draw
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant3JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk());

    // Now participant tries to join an OPEN draw (should fail)
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participantJwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Cannot join draw that is not in JOINING state"));
  }

  @Test
  public void participantFlow_cannotJoinSameDrawTwice_shouldFail() throws Exception {
    // Creator creates a draw
    String createDrawRequest =
        "{"
            + "\"title\": \"Duplicate Join Test\","
            + "\"description\": \"Test joining the same draw twice\","
            + "\"drawDate\": \"2024-12-20\","
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

    // Participant joins successfully
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participantJwtToken))
        .andExpect(status().isOk());

    // Participant tries to join again (should fail)
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participantJwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("User is already a participant in this draw"));
  }

  @Test
  public void participantFlow_cannotDrawInClosedDraw_shouldFail() throws Exception {
    // Create draw, add participants, but don't open it
    String createDrawRequest =
        "{"
            + "\"title\": \"Closed Draw Test\","
            + "\"description\": \"Test drawing in closed draw\","
            + "\"drawDate\": \"2024-12-20\","
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

    // Participants join
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participantJwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Participant tries to draw in JOINING state (should fail)
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participantJwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Draw is not open for drawing"));
  }

  @Test
  public void participantFlow_cannotViewResultBeforeDrawing_shouldFail() throws Exception {
    // Create and open a draw
    String createDrawRequest =
        "{"
            + "\"title\": \"No Result Test\","
            + "\"description\": \"Test viewing result before drawing\","
            + "\"drawDate\": \"2024-12-20\","
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

    // Participants join and draw is opened
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participantJwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk());

    // Participant tries to view result before drawing (should fail)
    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/my-result")
                .header("Authorization", participantJwtToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("User has not drawn a name in this draw yet"));
  }
}
