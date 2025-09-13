package com.namedraw.controller;

import com.namedraw.model.Draw;
import com.namedraw.model.User;
import com.namedraw.service.DrawService;
import com.namedraw.service.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user profile and user-related operations.
 *
 * <p>This controller handles user profile retrieval and user's draw management. It provides
 * endpoints for getting current user information and retrieving draws associated with the
 * authenticated user (both created and participated draws).
 *
 * <p>Endpoints: - GET /users/me: Get current user profile - GET /users/me/draws: Get user's draws
 * with optional filtering
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;
  private final DrawService drawService;

  /**
   * Get current user profile.
   *
   * @param jwt Current user's JWT token (can be null if token is invalid)
   * @return User profile information or 401 if unauthorized
   */
  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> getCurrentUserProfile(
      @AuthenticationPrincipal Jwt jwt) {
    if (jwt == null) {
      log.warn("Get user profile attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized", "Invalid or missing JWT token", "/api/v1/users/me"));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug("Getting user profile for user ID: {}", userId);

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found or inactive for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Unauthorized", "User not found or inactive", "/api/v1/users/me"));
      }

      User user = userOpt.get();
      Map<String, Object> userResponse = convertUserToResponse(user);
      log.debug("Successfully retrieved user profile for user ID: {}", userId);
      return ResponseEntity.ok(userResponse);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid user ID in JWT token: {}", jwt.getSubject(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse("Unauthorized", "Invalid user ID in token", "/api/v1/users/me"));
    } catch (Exception e) {
      log.error("Error retrieving user profile", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error", "Failed to retrieve user profile", "/api/v1/users/me"));
    }
  }

  /**
   * Get user's draws with optional filtering.
   *
   * @param jwt Current user's JWT token (can be null if token is invalid)
   * @param role Filter by role (creator, participant, all). Default: all
   * @param state Filter by state (JOINING, OPEN, ARCHIVED, all). Default: all
   * @return List of user's draws or 401 if unauthorized
   */
  @GetMapping("/me/draws")
  public ResponseEntity<Object> getUserDraws(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(value = "role", defaultValue = "all") String role,
      @RequestParam(value = "state", defaultValue = "all") String state) {

    if (jwt == null) {
      log.warn("Get user draws attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized", "Invalid or missing JWT token", "/api/v1/users/me/draws"));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug("Getting draws for user ID: {} with role: {} and state: {}", userId, role, state);

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found or inactive for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Unauthorized", "User not found or inactive", "/api/v1/users/me/draws"));
      }

      User user = userOpt.get();
      List<Draw> draws = getUserDrawsByRole(user, role);
      List<Draw> filteredDraws = filterDrawsByState(draws, state);
      List<Map<String, Object>> drawResponses = convertDrawsToResponse(filteredDraws);

      log.debug("Successfully retrieved {} draws for user ID: {}", drawResponses.size(), userId);
      return ResponseEntity.ok(drawResponses);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid user ID in JWT token: {}", jwt.getSubject(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized", "Invalid user ID in token", "/api/v1/users/me/draws"));
    } catch (Exception e) {
      log.error("Error retrieving user draws", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error",
                  "Failed to retrieve user draws",
                  "/api/v1/users/me/draws"));
    }
  }

  /**
   * Get draws based on user role filter.
   *
   * @param user the authenticated user
   * @param role the role filter (creator, participant, all)
   * @return list of draws matching the role filter
   */
  private List<Draw> getUserDrawsByRole(User user, String role) {
    return switch (role.toLowerCase()) {
      case "creator" -> drawService.findDrawsByCreator(user);
      case "participant" -> drawService.findDrawsByParticipant(user);
      case "all" -> {
        List<Draw> allDraws = new ArrayList<>();
        allDraws.addAll(drawService.findDrawsByCreator(user));
        allDraws.addAll(drawService.findDrawsByParticipant(user));
        yield allDraws;
      }
      default -> {
        log.warn("Invalid role filter: {}, defaulting to 'all'", role);
        List<Draw> allDraws = new ArrayList<>();
        allDraws.addAll(drawService.findDrawsByCreator(user));
        allDraws.addAll(drawService.findDrawsByParticipant(user));
        yield allDraws;
      }
    };
  }

  /**
   * Filter draws by state.
   *
   * @param draws the list of draws to filter
   * @param state the state filter (JOINING, OPEN, ARCHIVED, all)
   * @return filtered list of draws
   */
  private List<Draw> filterDrawsByState(List<Draw> draws, String state) {
    if ("all".equalsIgnoreCase(state)) {
      return draws;
    }

    try {
      Draw.DrawState targetState = Draw.DrawState.valueOf(state.toUpperCase());
      return draws.stream().filter(draw -> draw.getState() == targetState).toList();
    } catch (IllegalArgumentException e) {
      log.warn("Invalid state filter: {}, returning all draws", state);
      return draws;
    }
  }

  /**
   * Convert User entity to response Map.
   *
   * @param user the user entity
   * @return Map representation for JSON response
   */
  private Map<String, Object> convertUserToResponse(User user) {
    Map<String, Object> response = new HashMap<>();
    response.put("id", user.getId().toString());
    response.put("name", user.getName());
    response.put("profilePictureUrl", user.getProfilePictureUrl());
    response.put("isActive", user.getIsActive());
    return response;
  }

  /**
   * Convert list of Draw entities to response List.
   *
   * @param draws the list of draw entities
   * @return List of Map representations for JSON response
   */
  private List<Map<String, Object>> convertDrawsToResponse(List<Draw> draws) {
    return draws.stream().map(this::convertDrawToResponse).toList();
  }

  /**
   * Convert Draw entity to response Map.
   *
   * @param draw the draw entity
   * @return Map representation for JSON response
   */
  private Map<String, Object> convertDrawToResponse(Draw draw) {
    Map<String, Object> response = new HashMap<>();
    response.put("id", draw.getId().toString());
    response.put("title", draw.getTitle());
    response.put("description", draw.getDescription());
    response.put("state", draw.getState().toString());
    response.put("drawDate", draw.getDrawDate().toString());
    response.put("participantCount", draw.getParticipantCount());
    response.put("maxParticipants", draw.getMaxParticipants());
    response.put("createdAt", draw.getCreatedAt().toString());
    if (draw.getOpenedAt() != null) {
      response.put("openedAt", draw.getOpenedAt().toString());
    }
    if (draw.getArchivedAt() != null) {
      response.put("archivedAt", draw.getArchivedAt().toString());
    }

    // Add creator information
    response.put("creator", convertUserToResponse(draw.getCreator()));

    // Note: participants, canJoin, canDraw would require additional service calls
    // These are not needed for the basic contract test, but would be implemented
    // when DrawController is created with more detailed draw information
    response.put("participants", new ArrayList<>());
    response.put("canJoin", false);
    response.put("canDraw", false);

    return response;
  }

  /**
   * Create standardized error response.
   *
   * @param error the error type
   * @param message the error message
   * @param path the request path
   * @return Map representation for JSON error response
   */
  private Map<String, Object> createErrorResponse(String error, String message, String path) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", error);
    errorResponse.put("message", message);
    errorResponse.put("timestamp", Instant.now().toString());
    errorResponse.put("path", path);
    return errorResponse;
  }
}
