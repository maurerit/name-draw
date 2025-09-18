package com.namedraw.controller;

import com.namedraw.dto.CreateDrawRequest;
import com.namedraw.dto.DrawPageResponse;
import com.namedraw.dto.DrawResponse;
import com.namedraw.dto.ParticipationResponse;
import com.namedraw.dto.UpdateDrawRequest;
import com.namedraw.dto.UserResponse;
import com.namedraw.model.Draw;
import com.namedraw.model.Draw.DrawState;
import com.namedraw.model.Participation;
import com.namedraw.model.User;
import com.namedraw.service.DrawService;
import com.namedraw.service.UserService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for draw management operations.
 *
 * <p>This controller handles draw lifecycle management including creation, updating, participant
 * management, and state transitions. It provides endpoints for listing draws, creating new draws,
 * joining draws, and opening draws for drawing operations.
 *
 * <p>Endpoints: - GET /draws: List all draws with optional filtering and pagination - POST /draws:
 * Create new draw - GET /draws/{drawId}: Get draw details - PUT /draws/{drawId}: Update draw
 * (creator only, JOINING state only) - POST /draws/{drawId}/join: Join draw as participant - POST
 * /draws/{drawId}/open: Open draw for drawing (creator only)
 */
@RestController
@RequestMapping("/api/v1/draws")
@RequiredArgsConstructor
@Slf4j
public class DrawController {

  private final DrawService drawService;
  private final UserService userService;

  /**
   * List all draws with optional filtering and pagination.
   *
   * @param jwt Current user's JWT token
   * @param state Optional state filter (JOINING, OPEN, ARCHIVED)
   * @param page Page number (default: 0)
   * @param size Page size (default: 20, max: 100)
   * @return Paginated list of draws
   */
  @GetMapping
  public ResponseEntity<?> listDraws(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) DrawState state,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    if (jwt == null) {
      log.warn("List draws attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse("Unauthorized", "Invalid or missing JWT token", "/api/v1/draws"));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug(
          "Listing draws for user ID: {}, state: {}, page: {}, size: {}",
          userId,
          state,
          page,
          size);

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse("Unauthorized", "User not found or inactive", "/api/v1/draws"));
      }

      // Enforce maximum page size of 100
      if (size > 100) {
        log.warn("Requested page size {} exceeds maximum; returning 400", size);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "INVALID_ARGUMENT", "Page size cannot exceed 100", "/api/v1/draws"));
      }

      User currentUser = userOpt.get();
      List<Draw> allDraws = drawService.findAllDraws();

      // Filter by state if specified
      List<Draw> filteredDraws = allDraws;
      if (state != null) {
        filteredDraws = allDraws.stream().filter(draw -> draw.getState() == state).toList();
      }

      // Apply pagination manually
      int start = Math.min(page * size, filteredDraws.size());
      int end = Math.min(start + size, filteredDraws.size());
      List<Draw> pagedDraws = filteredDraws.subList(start, end);

      DrawPageResponse response =
          createDrawPageResponse(pagedDraws, filteredDraws.size(), page, size, currentUser);

      log.debug("Successfully retrieved {} draws for user ID: {}", filteredDraws.size(), userId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid user ID in JWT token: {}", jwt.getSubject(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(createErrorResponse("Unauthorized", "Invalid user ID in token", "/api/v1/draws"));
    } catch (Exception e) {
      log.error("Error listing draws", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error", "Failed to retrieve draws", "/api/v1/draws"));
    }
  }

  /**
   * Create a new draw.
   *
   * @param jwt Current user's JWT token
   * @param request Draw creation request
   * @return Created draw
   */
  @PostMapping
  public ResponseEntity<?> createDraw(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateDrawRequest request) {

    if (jwt == null) {
      log.warn("Create draw attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse("Unauthorized", "Invalid or missing JWT token", "/api/v1/draws"));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug("Creating draw for user ID: {}, title: {}", userId, request.getTitle());

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse("Unauthorized", "User not found or inactive", "/api/v1/draws"));
      }

      User creator = userOpt.get();
      Draw createdDraw =
          drawService.createDraw(
              creator,
              request.getTitle(),
              request.getDescription(),
              request.getDrawDate(),
              request.getMaxParticipants());

      DrawResponse response = convertDrawToResponse(createdDraw, creator);
      log.info(
          "Successfully created draw with ID: {} for user ID: {}", createdDraw.getId(), userId);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid request for create draw: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.APPLICATION_JSON)
          .body(createErrorResponse("Bad Request", e.getMessage(), "/api/v1/draws"));
    } catch (Exception e) {
      log.error("Error creating draw", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error", "Failed to create draw", "/api/v1/draws"));
    }
  }

  /**
   * Get draw details by ID.
   *
   * @param jwt Current user's JWT token
   * @param drawId Draw ID
   * @return Draw details
   */
  @GetMapping("/{drawId}")
  public ResponseEntity<?> getDrawById(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID drawId) {

    if (jwt == null) {
      log.warn("Get draw attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized", "Invalid or missing JWT token", "/api/v1/draws/" + drawId));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug("Getting draw ID: {} for user ID: {}", drawId, userId);

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Unauthorized", "User not found or inactive", "/api/v1/draws/" + drawId));
      }

      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        log.warn("Draw not found for ID: {}", drawId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createErrorResponse("Not Found", "Draw not found", "/api/v1/draws/" + drawId));
      }

      User currentUser = userOpt.get();
      Draw draw = drawOpt.get();
      DrawResponse response = convertDrawToResponse(draw, currentUser);

      log.debug("Successfully retrieved draw ID: {} for user ID: {}", drawId, userId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid user ID in JWT token: {}", jwt.getSubject(), e);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized", "Invalid user ID in token", "/api/v1/draws/" + drawId));
    } catch (Exception e) {
      log.error("Error retrieving draw ID: {}", drawId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error", "Failed to retrieve draw", "/api/v1/draws/" + drawId));
    }
  }

  /**
   * Update draw details (creator only, JOINING state only).
   *
   * @param jwt Current user's JWT token
   * @param drawId Draw ID
   * @param request Update request
   * @return Updated draw
   */
  @PutMapping("/{drawId}")
  public ResponseEntity<?> updateDraw(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID drawId,
      @Valid @RequestBody UpdateDrawRequest request) {

    if (jwt == null) {
      log.warn("Update draw attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized", "Invalid or missing JWT token", "/api/v1/draws/" + drawId));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug("Updating draw ID: {} for user ID: {}", drawId, userId);

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Unauthorized", "User not found or inactive", "/api/v1/draws/" + drawId));
      }

      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        log.warn("Draw not found for ID: {}", drawId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createErrorResponse("Not Found", "Draw not found", "/api/v1/draws/" + drawId));
      }

      User currentUser = userOpt.get();
      Draw draw = drawOpt.get();

      // Check if user is the creator
      if (!draw.getCreator().getId().equals(currentUser.getId())) {
        log.warn(
            "User ID: {} attempted to update draw ID: {} but is not the creator", userId, drawId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Forbidden",
                    "Only the draw creator can update the draw",
                    "/api/v1/draws/" + drawId));
      }

      // Check if draw is in JOINING state
      if (draw.getState() != DrawState.JOINING) {
        log.warn("Attempted to update draw ID: {} in state: {}", drawId, draw.getState());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Forbidden",
                    "Draw can only be updated in JOINING state",
                    "/api/v1/draws/" + drawId));
      }

      Draw updatedDraw =
          drawService.updateDraw(
              drawId,
              currentUser,
              request.getTitle(),
              request.getDescription(),
              request.getDrawDate(),
              request.getMaxParticipants());
      DrawResponse response = convertDrawToResponse(updatedDraw, currentUser);

      log.info("Successfully updated draw ID: {} for user ID: {}", drawId, userId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid request for update draw: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.APPLICATION_JSON)
          .body(createErrorResponse("Bad Request", e.getMessage(), "/api/v1/draws/" + drawId));
    } catch (Exception e) {
      log.error("Error updating draw ID: {}", drawId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error", "Failed to update draw", "/api/v1/draws/" + drawId));
    }
  }

  /**
   * Join a draw as a participant.
   *
   * @param jwt Current user's JWT token
   * @param drawId Draw ID
   * @return Participation confirmation
   */
  @PostMapping("/{drawId}/join")
  public ResponseEntity<?> joinDraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID drawId) {

    if (jwt == null) {
      log.warn("Join draw attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized",
                  "Invalid or missing JWT token",
                  "/api/v1/draws/" + drawId + "/join"));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug("User ID: {} attempting to join draw ID: {}", userId, drawId);

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Unauthorized",
                    "User not found or inactive",
                    "/api/v1/draws/" + drawId + "/join"));
      }

      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        log.warn("Draw not found for ID: {}", drawId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Not Found", "Draw not found", "/api/v1/draws/" + drawId + "/join"));
      }

      User participant = userOpt.get();

      Participation participation = drawService.joinDraw(drawId, participant);
      ParticipationResponse response = convertParticipationToResponse(participation);

      log.info("Successfully joined user ID: {} to draw ID: {}", userId, drawId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid request for join draw: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Bad Request", e.getMessage(), "/api/v1/draws/" + drawId + "/join"));
    } catch (IllegalStateException e) {
      // Business rule violations (e.g., wrong state, at capacity)
      log.warn("Cannot join draw ID {}: {}", drawId, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Bad Request", e.getMessage(), "/api/v1/draws/" + drawId + "/join"));
    } catch (Exception e) {
      log.error("Error joining draw ID: {}", drawId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error",
                  "Failed to join draw",
                  "/api/v1/draws/" + drawId + "/join"));
    }
  }

  /**
   * Open draw for drawing operations (creator only).
   *
   * @param jwt Current user's JWT token
   * @param drawId Draw ID
   * @return Updated draw in OPEN state
   */
  @PostMapping("/{drawId}/open")
  public ResponseEntity<?> openDraw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID drawId) {

    if (jwt == null) {
      log.warn("Open draw attempted with invalid or missing JWT token");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Unauthorized",
                  "Invalid or missing JWT token",
                  "/api/v1/draws/" + drawId + "/open"));
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      log.debug("User ID: {} attempting to open draw ID: {}", userId, drawId);

      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        log.warn("User not found for ID: {}", userId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Unauthorized",
                    "User not found or inactive",
                    "/api/v1/draws/" + drawId + "/open"));
      }

      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        log.warn("Draw not found for ID: {}", drawId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Not Found", "Draw not found", "/api/v1/draws/" + drawId + "/open"));
      }

      User currentUser = userOpt.get();
      Draw draw = drawOpt.get();

      // Check if user is the creator
      if (!draw.getCreator().getId().equals(currentUser.getId())) {
        log.warn(
            "User ID: {} attempted to open draw ID: {} but is not the creator", userId, drawId);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                createErrorResponse(
                    "Forbidden",
                    "Only the draw creator can open the draw",
                    "/api/v1/draws/" + drawId + "/open"));
      }

      Draw openedDraw = drawService.openDrawForDrawing(drawId, currentUser);
      DrawResponse response = convertDrawToResponse(openedDraw, currentUser);

      log.info("Successfully opened draw ID: {} by user ID: {}", drawId, userId);
      return ResponseEntity.ok(response);

    } catch (IllegalArgumentException e) {
      log.warn("Invalid request for open draw: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Bad Request", e.getMessage(), "/api/v1/draws/" + drawId + "/open"));
    } catch (IllegalStateException e) {
      // Business rule violations (e.g., wrong state, insufficient participants)
      log.warn("Cannot open draw ID {}: {}", drawId, e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Bad Request", e.getMessage(), "/api/v1/draws/" + drawId + "/open"));
    } catch (Exception e) {
      log.error("Error opening draw ID: {}", drawId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .contentType(MediaType.APPLICATION_JSON)
          .body(
              createErrorResponse(
                  "Internal Server Error",
                  "Failed to open draw",
                  "/api/v1/draws/" + drawId + "/open"));
    }
  }

  // Helper methods for data conversion and error responses

  private Map<String, Object> createErrorResponse(String error, String message, String path) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", error);
    errorResponse.put("message", message);
    errorResponse.put("timestamp", Instant.now().toString());
    errorResponse.put("path", path);
    return errorResponse;
  }

  private DrawResponse convertDrawToResponse(Draw draw, User currentUser) {
    DrawResponse response = new DrawResponse();
    response.setId(draw.getId());
    response.setTitle(draw.getTitle());
    response.setDescription(draw.getDescription());
    response.setState(draw.getState());
    response.setDrawDate(draw.getDrawDate());
    response.setParticipantCount(draw.getParticipantCount());
    response.setMaxParticipants(draw.getMaxParticipants());
    response.setCreatedAt(draw.getCreatedAt());
    response.setOpenedAt(draw.getOpenedAt());
    response.setArchivedAt(draw.getArchivedAt());

    // Convert creator
    response.setCreator(convertUserToResponse(draw.getCreator()));

    // Convert participants
    List<UserResponse> participantResponses = new ArrayList<>();
    if (draw.getParticipations() != null) {
      for (Participation participation : draw.getParticipations()) {
        participantResponses.add(convertUserToResponse(participation.getUser()));
      }
    }
    response.setParticipants(participantResponses);

    // Set permission flags
    response.setCanJoin(canUserJoinDraw(draw, currentUser));
    response.setCanDraw(canUserDrawFromDraw(draw, currentUser));

    return response;
  }

  private UserResponse convertUserToResponse(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setName(user.getName());
    response.setProfilePictureUrl(user.getProfilePictureUrl());
    response.setIsActive(user.getIsActive());
    return response;
  }

  private ParticipationResponse convertParticipationToResponse(Participation participation) {
    ParticipationResponse response = new ParticipationResponse();
    response.setId(participation.getId());
    response.setJoinedAt(participation.getJoinedAt());

    // Convert draw (without participants to avoid circular reference)
    DrawResponse drawResponse = new DrawResponse();
    Draw draw = participation.getDraw();
    drawResponse.setId(draw.getId());
    drawResponse.setTitle(draw.getTitle());
    drawResponse.setDescription(draw.getDescription());
    drawResponse.setState(draw.getState());
    drawResponse.setDrawDate(draw.getDrawDate());
    drawResponse.setParticipantCount(draw.getParticipantCount());
    drawResponse.setMaxParticipants(draw.getMaxParticipants());
    drawResponse.setCreatedAt(draw.getCreatedAt());
    drawResponse.setOpenedAt(draw.getOpenedAt());
    drawResponse.setArchivedAt(draw.getArchivedAt());
    drawResponse.setCreator(convertUserToResponse(draw.getCreator()));
    drawResponse.setParticipants(new ArrayList<>());
    drawResponse.setCanJoin(false);
    drawResponse.setCanDraw(false);

    response.setDraw(drawResponse);
    return response;
  }

  private DrawPageResponse createDrawPageResponse(
      List<Draw> draws, int totalElements, int page, int size, User currentUser) {
    DrawPageResponse response = new DrawPageResponse();

    List<DrawResponse> drawResponses = new ArrayList<>();
    for (Draw draw : draws) {
      drawResponses.add(convertDrawToResponse(draw, currentUser));
    }

    response.setContent(drawResponses);
    response.setTotalElements((long) totalElements);
    response.setTotalPages((int) Math.ceil((double) totalElements / size));
    response.setSize(size);
    response.setNumber(page);
    response.setFirst(page == 0);
    response.setLast((page + 1) * size >= totalElements);

    return response;
  }

  private boolean canUserJoinDraw(Draw draw, User user) {
    // User can join if:
    // 1. Draw is in JOINING state
    // 2. User is not already a participant
    // 3. Draw has capacity for more participants
    if (draw.getState() != DrawState.JOINING) {
      return false;
    }

    if (draw.getParticipantCount() >= draw.getMaxParticipants()) {
      return false;
    }

    // Check if user is already a participant
    if (draw.getParticipations() != null) {
      for (Participation participation : draw.getParticipations()) {
        if (participation.getUser().getId().equals(user.getId())) {
          return false;
        }
      }
    }

    return true;
  }

  private boolean canUserDrawFromDraw(Draw draw, User user) {
    // User can draw if:
    // 1. Draw is in OPEN state
    // 2. User is a participant
    // 3. User hasn't already drawn a name
    if (draw.getState() != DrawState.OPEN) {
      return false;
    }

    // Check if user is a participant
    boolean isParticipant = false;
    if (draw.getParticipations() != null) {
      for (Participation participation : draw.getParticipations()) {
        if (participation.getUser().getId().equals(user.getId())) {
          isParticipant = true;
          break;
        }
      }
    }

    if (!isParticipant) {
      return false;
    }

    // This would need to check if user has already drawn - requires DrawQueue or DrawnName service
    // For now, assume they can draw (this logic belongs in DrawingService)
    return true;
  }
}
