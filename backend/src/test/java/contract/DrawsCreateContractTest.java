package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract test for POST /draws endpoint
 *
 * <p>This test verifies the API contract for creating a new draw. Expected to FAIL until
 * DrawController is implemented.
 *
 * <p>API Contract: - POST /api/v1/draws - Requires Bearer authentication - Request body:
 * CreateDrawRequest JSON - Returns 201 with Draw JSON on success - Returns 400 if request is
 * invalid - Returns 401 if unauthorized - CreateDrawRequest schema: {title: string (1-100 chars),
 * description?: string (max 500 chars), drawDate: date, maxParticipants: integer (2-30, default
 * 30)} - Draw schema: {id: uuid, title: string, description?: string, state: enum, drawDate: date,
 * participantCount: number, maxParticipants: number, creator: User, participants: User[], canJoin:
 * boolean, canDraw: boolean, createdAt: datetime, openedAt?: datetime, archivedAt?: datetime}
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class DrawsCreateContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void createDraw_withValidRequest_shouldReturn201WithDrawJson() throws Exception {
    // Given: Valid JWT token and create draw request
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String createDrawRequest =
        "{"
            + "\"title\": \"Family Christmas Draw\","
            + "\"description\": \"Annual family Christmas name drawing\","
            + "\"drawDate\": \"2024-12-15\","
            + "\"maxParticipants\": 10"
            + "}";

    // When: POST /api/v1/draws
    // Then: Should return 201 with Draw JSON
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("Family Christmas Draw"))
        .andExpect(jsonPath("$.description").value("Annual family Christmas name drawing"))
        .andExpect(jsonPath("$.state").exists())
        .andExpect(jsonPath("$.drawDate").value("2024-12-15"))
        .andExpect(jsonPath("$.participantCount").exists())
        .andExpect(jsonPath("$.participantCount").isNumber())
        .andExpect(jsonPath("$.maxParticipants").value(10))
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
  public void createDraw_withMinimalRequest_shouldReturn201WithDrawJson() throws Exception {
    // Given: Valid JWT token and minimal create draw request (only required fields)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String createDrawRequest =
        "{" + "\"title\": \"Simple Draw\"," + "\"drawDate\": \"2024-12-20\"" + "}";

    // When: POST /api/v1/draws
    // Then: Should return 201 with Draw JSON and default values
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("Simple Draw"))
        .andExpect(jsonPath("$.description").doesNotExist())
        .andExpect(jsonPath("$.drawDate").value("2024-12-20"))
        .andExpect(jsonPath("$.maxParticipants").value(30)); // Default value
  }

  @Test
  public void createDraw_withoutToken_shouldReturn401() throws Exception {
    // Given: Create draw request without authentication
    String createDrawRequest =
        "{" + "\"title\": \"Unauthorized Draw\"," + "\"drawDate\": \"2024-12-15\"" + "}";

    // When: POST /api/v1/draws without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            post("/api/v1/draws")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token and create draw request
    String invalidJwtToken = "Bearer invalid.token.here";
    String createDrawRequest =
        "{" + "\"title\": \"Invalid Token Draw\"," + "\"drawDate\": \"2024-12-15\"" + "}";

    // When: POST /api/v1/draws with invalid token
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", invalidJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withMissingTitle_shouldReturn400() throws Exception {
    // Given: Valid JWT token but request missing required title
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String createDrawRequest =
        "{" + "\"description\": \"Draw without title\"," + "\"drawDate\": \"2024-12-15\"" + "}";

    // When: POST /api/v1/draws without required title
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withMissingDrawDate_shouldReturn400() throws Exception {
    // Given: Valid JWT token but request missing required drawDate
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String createDrawRequest =
        "{"
            + "\"title\": \"Draw without date\","
            + "\"description\": \"Draw missing draw date\""
            + "}";

    // When: POST /api/v1/draws without required drawDate
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withTitleTooLong_shouldReturn400() throws Exception {
    // Given: Valid JWT token but title exceeds 100 characters
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String longTitle = "A".repeat(101); // 101 characters, exceeds 100 char limit
    String createDrawRequest =
        "{" + "\"title\": \"" + longTitle + "\"," + "\"drawDate\": \"2024-12-15\"" + "}";

    // When: POST /api/v1/draws with title too long
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withDescriptionTooLong_shouldReturn400() throws Exception {
    // Given: Valid JWT token but description exceeds 500 characters
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String longDescription = "A".repeat(501); // 501 characters, exceeds 500 char limit
    String createDrawRequest =
        "{"
            + "\"title\": \"Valid Title\","
            + "\"description\": \""
            + longDescription
            + "\","
            + "\"drawDate\": \"2024-12-15\""
            + "}";

    // When: POST /api/v1/draws with description too long
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withMaxParticipantsTooLow_shouldReturn400() throws Exception {
    // Given: Valid JWT token but maxParticipants below minimum (2)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String createDrawRequest =
        "{"
            + "\"title\": \"Invalid Participants Draw\","
            + "\"drawDate\": \"2024-12-15\","
            + "\"maxParticipants\": 1"
            + "}";

    // When: POST /api/v1/draws with maxParticipants too low
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withMaxParticipantsTooHigh_shouldReturn400() throws Exception {
    // Given: Valid JWT token but maxParticipants above maximum (30)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String createDrawRequest =
        "{"
            + "\"title\": \"Too Many Participants Draw\","
            + "\"drawDate\": \"2024-12-15\","
            + "\"maxParticipants\": 31"
            + "}";

    // When: POST /api/v1/draws with maxParticipants too high
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withInvalidDateFormat_shouldReturn400() throws Exception {
    // Given: Valid JWT token but invalid date format
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String createDrawRequest =
        "{"
            + "\"title\": \"Invalid Date Draw\","
            + "\"drawDate\": \"12/15/2024\"" // Invalid format, should be YYYY-MM-DD
            + "}";

    // When: POST /api/v1/draws with invalid date format
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createDrawRequest))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withEmptyBody_shouldReturn400() throws Exception {
    // Given: Valid JWT token but empty request body
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: POST /api/v1/draws with empty body
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void createDraw_withMalformedJson_shouldReturn400() throws Exception {
    // Given: Valid JWT token but malformed JSON
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    String malformedJson = "{\"title\": \"Test Draw\", \"drawDate\": "; // Missing closing

    // When: POST /api/v1/draws with malformed JSON
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            post("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }
}
