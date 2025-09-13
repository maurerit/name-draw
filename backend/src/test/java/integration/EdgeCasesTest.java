package integration;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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
 * Integration test for edge cases and concurrent access scenarios
 *
 * <p>This test verifies the system's behavior in edge cases and concurrent scenarios. Expected to
 * FAIL until all components (controllers, services, repositories) are implemented.
 *
 * <p>Edge cases tested: 1. Self-draws prevention and automatic re-drawing 2. Concurrent access and
 * thread safety 3. Single participant draw handling 4. Maximum participant auto-transition 5.
 * Various error conditions and boundary cases 6. Draw queue management under concurrent load
 *
 * <p>Business rules verified: - System prevents self-draws and automatically re-draws - Concurrent
 * drawing requests are handled safely - Single participant draws are handled appropriately - Max
 * participant draws auto-transition to OPEN state - System maintains data consistency under
 * concurrent access - Edge cases fail gracefully with appropriate error messages
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class EdgeCasesTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  private String creatorJwtToken;
  private String participant1JwtToken;
  private String participant2JwtToken;
  private String participant3JwtToken;
  private String participant4JwtToken;

  @BeforeEach
  public void setUp() {
    // Mock JWT tokens for different users
    // In real implementation, these would be generated through OAuth flow
    creatorJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjcmVhdG9yLXVzZXItaWQiLCJuYW1lIjoiSm9obiBDcmVhdG9yIiwiaWF0IjoxNTE2MjM5MDIyfQ.creator-jwt-token";
    participant1JwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXJ0aWNpcGFudC0xLWlkIiwibmFtZSI6IkFsaWNlIFBhcnRpY2lwYW50IiwiaWF0IjoxNTE2MjM5MDIyfQ.participant1-jwt-token";
    participant2JwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXJ0aWNpcGFudC0yLWlkIiwibmFtZSI6IkJvYiBQYXJ0aWNpcGFudCIsImlhdCI6MTUxNjIzOTAyMn0.participant2-jwt-token";
    participant3JwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXJ0aWNpcGFudC0zLWlkIiwibmFtZSI6IkNoYXJsaWUgUGFydGljaXBhbnQiLCJpYXQiOjE1MTYyMzkwMjJ9.participant3-jwt-token";
    participant4JwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJwYXJ0aWNpcGFudC00LWlkIiwibmFtZSI6IkRhdmlkIFBhcnRpY2lwYW50IiwiaWF0IjoxNTE2MjM5MDIyfQ.participant4-jwt-token";
  }

  @Test
  public void selfDrawPrevention_twoParticipantDraw_shouldAutomaticallyRedraw() throws Exception {
    // Test Case: With only 2 participants, system should handle self-draws gracefully
    // Expected behavior: If a self-draw occurs, system should automatically re-draw

    String createDrawRequest =
        "{"
            + "\"title\": \"Self-Draw Prevention Test\","
            + "\"description\": \"Testing automatic re-draw on self-draw\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add both participants
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Open the draw
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk());

    // Both participants draw names
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

    // Verify no self-draws occurred
    String draw1ResponseJson = draw1Result.getResponse().getContentAsString();
    JsonNode draw1ResponseNode = objectMapper.readTree(draw1ResponseJson);
    String drawnUser1Id = draw1ResponseNode.get("drawnUser").get("id").asText();

    String draw2ResponseJson = draw2Result.getResponse().getContentAsString();
    JsonNode draw2ResponseNode = objectMapper.readTree(draw2ResponseJson);
    String drawnUser2Id = draw2ResponseNode.get("drawnUser").get("id").asText();

    // Verify participant 1 drew participant 2
    assertEquals("participant-2-id", drawnUser1Id, "Participant 1 should draw participant 2");

    // Verify participant 2 drew participant 1
    assertEquals("participant-1-id", drawnUser2Id, "Participant 2 should draw participant 1");
  }

  @Test
  public void concurrentDrawing_multipleParticipants_shouldHandleThreadSafety() throws Exception {
    // Test Case: Multiple participants try to draw simultaneously
    // Expected behavior: All draws should complete successfully without data corruption

    String createDrawRequest =
        "{"
            + "\"title\": \"Concurrent Access Test\","
            + "\"description\": \"Testing thread safety during concurrent drawing\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add 4 participants
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant3JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant4JwtToken))
        .andExpect(status().isOk());

    // Open the draw
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk());

    // Create concurrent drawing tasks
    ExecutorService executor = Executors.newFixedThreadPool(4);
    List<Future<MvcResult>> futures = new ArrayList<>();

    String[] tokens = {
      participant1JwtToken, participant2JwtToken, participant3JwtToken, participant4JwtToken
    };

    // Submit concurrent drawing requests
    for (String token : tokens) {
      Future<MvcResult> future =
          executor.submit(
              () -> {
                try {
                  return mockMvc
                      .perform(
                          post("/api/v1/draws/" + drawId + "/draw").header("Authorization", token))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.drawnUser").exists())
                      .andExpect(jsonPath("$.drawnAt").exists())
                      .andReturn();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
      futures.add(future);
    }

    // Wait for all draws to complete
    List<String> drawnUserIds = new ArrayList<>();
    for (Future<MvcResult> future : futures) {
      MvcResult result = future.get(10, TimeUnit.SECONDS);
      String responseJson = result.getResponse().getContentAsString();
      JsonNode responseNode = objectMapper.readTree(responseJson);
      String drawnUserId = responseNode.get("drawnUser").get("id").asText();
      drawnUserIds.add(drawnUserId);
    }

    executor.shutdown();

    // Verify no duplicate draws occurred (each participant should be drawn exactly once)
    assertEquals(4, drawnUserIds.size(), "All 4 participants should have drawn names");
    assertEquals(4, drawnUserIds.stream().distinct().count(), "All drawn names should be unique");

    // Verify results are accessible
    mockMvc
        .perform(
            get("/api/v1/draws/" + drawId + "/results").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$", hasSize(4)));
  }

  @Test
  public void singleParticipantDraw_shouldPreventOpening() throws Exception {
    // Test Case: Draw with only one participant
    // Expected behavior: System should prevent opening draw with single participant

    String createDrawRequest =
        "{"
            + "\"title\": \"Single Participant Test\","
            + "\"description\": \"Testing single participant edge case\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add only one participant
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    // Attempt to open draw with single participant should fail
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Cannot open draw with only one participant"));

    // Verify draw remains in JOINING state
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("JOINING"))
        .andExpect(jsonPath("$.participantCount").value(1));
  }

  @Test
  public void maxParticipantLimit_shouldRejectExtraParticipants() throws Exception {
    // Test Case: Attempting to exceed maximum participant limit
    // Expected behavior: System should reject participants beyond the limit

    String createDrawRequest =
        "{"
            + "\"title\": \"Max Participants Test\","
            + "\"description\": \"Testing maximum participant enforcement\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add first two participants (should succeed and auto-open)
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Verify draw auto-opened
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.participantCount").value(2));

    // Attempt to add third participant should fail
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant3JwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Cannot join draw that is not in JOINING state"));
  }

  @Test
  public void doubleDrawAttempt_shouldPreventMultipleDraws() throws Exception {
    // Test Case: Participant tries to draw twice
    // Expected behavior: Second draw attempt should be rejected

    String createDrawRequest =
        "{"
            + "\"title\": \"Double Draw Prevention Test\","
            + "\"description\": \"Testing prevention of multiple draws per participant\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add participants
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Open the draw
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk());

    // First draw should succeed
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.drawnUser").exists())
        .andExpect(jsonPath("$.drawnAt").exists());

    // Second draw attempt should fail
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participant1JwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("User has already drawn a name in this draw"));
  }

  @Test
  public void drawBeforeOpen_shouldRejectDrawAttempt() throws Exception {
    // Test Case: Participant tries to draw before draw is opened
    // Expected behavior: Draw attempt should be rejected

    String createDrawRequest =
        "{"
            + "\"title\": \"Draw Before Open Test\","
            + "\"description\": \"Testing draw rejection when draw not open\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add participants
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Attempt to draw before opening should fail
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participant1JwtToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Cannot draw from a draw that is not open"));
  }

  @Test
  public void nonParticipantDrawAttempt_shouldRejectAccess() throws Exception {
    // Test Case: Non-participant tries to draw
    // Expected behavior: Draw attempt should be rejected

    String createDrawRequest =
        "{"
            + "\"title\": \"Non-Participant Test\","
            + "\"description\": \"Testing non-participant draw rejection\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add participants (but not participant3)
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant2JwtToken))
        .andExpect(status().isOk());

    // Open the draw
    mockMvc
        .perform(post("/api/v1/draws/" + drawId + "/open").header("Authorization", creatorJwtToken))
        .andExpect(status().isOk());

    // Non-participant attempt to draw should fail
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/draw").header("Authorization", participant3JwtToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("User is not a participant in this draw"));
  }

  @Test
  public void concurrentJoining_atMaxCapacity_shouldHandleRaceCondition() throws Exception {
    // Test Case: Multiple users try to join when near max capacity
    // Expected behavior: System should handle race conditions gracefully

    String createDrawRequest =
        "{"
            + "\"title\": \"Concurrent Joining Test\","
            + "\"description\": \"Testing concurrent joining race conditions\","
            + "\"drawDate\": \"2024-12-15\","
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

    // Add first participant
    mockMvc
        .perform(
            post("/api/v1/draws/" + drawId + "/join").header("Authorization", participant1JwtToken))
        .andExpect(status().isOk());

    // Try to add multiple participants concurrently for the last spot
    ExecutorService executor = Executors.newFixedThreadPool(3);
    List<Future<Integer>> futures = new ArrayList<>();

    String[] tokens = {participant2JwtToken, participant3JwtToken, participant4JwtToken};

    // Submit concurrent join requests
    for (String token : tokens) {
      Future<Integer> future =
          executor.submit(
              () -> {
                try {
                  MvcResult result =
                      mockMvc
                          .perform(
                              post("/api/v1/draws/" + drawId + "/join")
                                  .header("Authorization", token))
                          .andReturn();
                  return result.getResponse().getStatus();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
      futures.add(future);
    }

    // Collect results
    List<Integer> statuses = new ArrayList<>();
    for (Future<Integer> future : futures) {
      statuses.add(future.get(10, TimeUnit.SECONDS));
    }

    executor.shutdown();

    // Verify exactly one success (200) and two failures (400)
    long successCount = statuses.stream().filter(status -> status == 200).count();
    long failureCount = statuses.stream().filter(status -> status == 400).count();

    assertEquals(1, successCount, "Exactly one participant should succeed in joining");
    assertEquals(2, failureCount, "Exactly two participants should fail to join");

    // Verify final state - draw should be open with 2 participants
    mockMvc
        .perform(get("/api/v1/draws/" + drawId).header("Authorization", creatorJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("OPEN"))
        .andExpect(jsonPath("$.participantCount").value(2));
  }
}
