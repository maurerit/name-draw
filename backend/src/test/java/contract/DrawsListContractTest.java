package contract;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract test for GET /draws endpoint
 *
 * <p>This test verifies the API contract for getting all draws. Expected to FAIL until
 * DrawController is implemented.
 *
 * <p>API Contract: - GET /api/v1/draws - Query parameters: state=[JOINING|OPEN|ARCHIVED],
 * page=integer, size=integer - Requires Bearer authentication - Returns 200 with DrawPage JSON on
 * success - Returns 401 if unauthorized - DrawPage schema: {content: Draw[], totalElements: number,
 * totalPages: number, size: number, number: number, first: boolean, last: boolean} - Draw schema:
 * {id: uuid, title: string, description?: string, state: enum, drawDate: date, participantCount:
 * number, maxParticipants: number, creator: User, participants: User[], canJoin: boolean, canDraw:
 * boolean, createdAt: datetime, openedAt?: datetime, archivedAt?: datetime}
 */
@SpringBootTest(classes = com.namedraw.NameDrawApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class DrawsListContractTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void getDraws_withValidToken_shouldReturn200WithDrawPage() throws Exception {
    // Given: Valid JWT token for authenticated user
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/draws
    // Then: Should return 200 with DrawPage JSON
    mockMvc
        .perform(
            get("/api/v1/draws")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").exists())
        .andExpect(jsonPath("$.totalElements").isNumber())
        .andExpect(jsonPath("$.totalPages").exists())
        .andExpect(jsonPath("$.totalPages").isNumber())
        .andExpect(jsonPath("$.size").exists())
        .andExpect(jsonPath("$.size").isNumber())
        .andExpect(jsonPath("$.number").exists())
        .andExpect(jsonPath("$.number").isNumber())
        .andExpect(jsonPath("$.first").exists())
        .andExpect(jsonPath("$.first").isBoolean())
        .andExpect(jsonPath("$.last").exists())
        .andExpect(jsonPath("$.last").isBoolean());
  }

  @Test
  public void getDraws_withStateFilter_shouldReturn200WithFilteredResults() throws Exception {
    // Given: Valid JWT token and state filter
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/draws?state=JOINING
    // Then: Should return 200 with DrawPage JSON
    mockMvc
        .perform(
            get("/api/v1/draws")
                .param("state", "JOINING")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  public void getDraws_withPaginationParams_shouldReturn200WithPaginatedResults() throws Exception {
    // Given: Valid JWT token and pagination parameters
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/draws?page=0&size=10
    // Then: Should return 200 with paginated DrawPage JSON
    mockMvc
        .perform(
            get("/api/v1/draws")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.number").value(0));
  }

  @Test
  public void getDraws_withoutToken_shouldReturn401() throws Exception {
    // When: GET /api/v1/draws without Authorization header
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(get("/api/v1/draws").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDraws_withInvalidToken_shouldReturn401() throws Exception {
    // Given: Invalid JWT token
    String invalidJwtToken = "Bearer invalid.token.here";

    // When: GET /api/v1/draws with invalid token
    // Then: Should return 401 Unauthorized
    mockMvc
        .perform(
            get("/api/v1/draws")
                .header("Authorization", invalidJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDraws_withInvalidStateParam_shouldReturn400() throws Exception {
    // Given: Valid JWT token and invalid state parameter
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/draws?state=INVALID_STATE
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            get("/api/v1/draws")
                .param("state", "INVALID_STATE")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  public void getDraws_withExcessiveSizeParam_shouldReturn400() throws Exception {
    // Given: Valid JWT token and excessive size parameter (>100)
    String validJwtToken =
        "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    // When: GET /api/v1/draws?size=200 (exceeds maximum of 100)
    // Then: Should return 400 Bad Request
    mockMvc
        .perform(
            get("/api/v1/draws")
                .param("size", "200")
                .header("Authorization", validJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.message").exists());
  }
}
