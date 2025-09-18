package com.namedraw.controller;

import com.namedraw.dto.DrawResultResponse;
import com.namedraw.dto.DrawResultResponse.UserDetailResponse;
import com.namedraw.model.Draw;
import com.namedraw.model.Draw.DrawState;
import com.namedraw.model.DrawnName;
import com.namedraw.model.User;
import com.namedraw.service.DrawService;
import com.namedraw.service.DrawingService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for drawing operations.
 *
 * <p>Provides endpoints to perform a draw, retrieve all results for a draw, and fetch the
 * authenticated user's own draw result.
 */
@RestController
@RequestMapping("/api/v1/draws")
@RequiredArgsConstructor
@Slf4j
public class DrawingController {

  private final DrawingService drawingService;
  private final DrawService drawService;
  private final UserService userService;

  /**
   * Perform a name draw for the authenticated user in the specified draw.
   *
   * @param jwt authenticated principal token
   * @param drawId target draw identifier
   * @return 200 with draw result or appropriate error response
   */
  @PostMapping("/{drawId}/draw")
  public ResponseEntity<?> performDraw(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID drawId) {

    if (jwt == null) {
      return unauthorized("/api/v1/draws/" + drawId + "/draw");
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        return unauthorized("/api/v1/draws/" + drawId + "/draw");
      }
      User user = userOpt.get();

      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        return notFound("/api/v1/draws/" + drawId + "/draw");
      }
      // Must be a participant to draw
      if (!drawService.isUserParticipant(drawId, user)) {
        return forbidden("/api/v1/draws/" + drawId + "/draw", "User is not a participant");
      }

      try {
        DrawnName result = drawingService.performDraw(drawId, user);
        return ResponseEntity.ok(convertToResponse(result));
      } catch (RuntimeException e) {
        // Determine if it's a conflict (queue lock) vs bad request
        String msg = e.getMessage() != null ? e.getMessage() : "Conflict";
        if (msg.toLowerCase().contains("another draw is already in progress")) {
          return conflict("/api/v1/draws/" + drawId + "/draw", msg);
        }
        return badRequest("/api/v1/draws/" + drawId + "/draw", msg);
      }
    } catch (IllegalArgumentException ex) {
      // invalid UUID in token or other argument errors
      return badRequest("/api/v1/draws/" + drawId + "/draw", ex.getMessage());
    }
  }

  /**
   * Retrieve all draw results for the specified draw.
   *
   * @param jwt authenticated principal token
   * @param drawId target draw identifier
   * @return 200 with list of results or appropriate error response
   */
  @GetMapping("/{drawId}/results")
  public ResponseEntity<?> getResults(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID drawId) {

    if (jwt == null) {
      return unauthorized("/api/v1/draws/" + drawId + "/results");
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        return unauthorized("/api/v1/draws/" + drawId + "/results");
      }
      User user = userOpt.get();

      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        return notFound("/api/v1/draws/" + drawId + "/results");
      }
      Draw draw = drawOpt.get();

      // Results are visible if draw is archived; otherwise only to participants
      if (draw.getState() != DrawState.ARCHIVED && !drawService.isUserParticipant(drawId, user)) {
        return forbidden("/api/v1/draws/" + drawId + "/results", "Not authorized to view results");
      }

      List<DrawnName> results = drawingService.getDrawResults(drawId);
      List<DrawResultResponse> response = new ArrayList<>();
      for (DrawnName dn : results) {
        response.add(convertToResponse(dn));
      }
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException ex) {
      return badRequest("/api/v1/draws/" + drawId + "/results", ex.getMessage());
    }
  }

  /**
   * Retrieve the authenticated user's draw result for the specified draw.
   *
   * @param jwt authenticated principal token
   * @param drawId target draw identifier
   * @return 200 with user's result or 404 if not drawn yet
   */
  @GetMapping("/{drawId}/my-result")
  public ResponseEntity<?> getMyResult(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID drawId) {

    if (jwt == null) {
      return unauthorized("/api/v1/draws/" + drawId + "/my-result");
    }

    try {
      UUID userId = UUID.fromString(jwt.getSubject());
      Optional<User> userOpt = userService.findById(userId);
      if (userOpt.isEmpty()) {
        return unauthorized("/api/v1/draws/" + drawId + "/my-result");
      }
      User user = userOpt.get();

      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        return notFound("/api/v1/draws/" + drawId + "/my-result");
      }

      // Must be a participant
      if (!drawService.isUserParticipant(drawId, user)) {
        return forbidden("/api/v1/draws/" + drawId + "/my-result", "User is not a participant");
      }

      Optional<DrawnName> my = drawingService.getMyDrawResult(drawId, user);
      if (my.isEmpty()) {
        return notFound("/api/v1/draws/" + drawId + "/my-result");
      }
      return ResponseEntity.ok(convertToResponse(my.get()));
    } catch (IllegalArgumentException ex) {
      return badRequest("/api/v1/draws/" + drawId + "/my-result", ex.getMessage());
    }
  }

  private DrawResultResponse convertToResponse(DrawnName dn) {
    User drawn = dn.getDrawnUser();
    UserDetailResponse userDto =
        new UserDetailResponse(
            drawn.getId(),
            drawn.getName(),
            drawn.getEmail(),
            drawn.getProfilePictureUrl(),
            drawn.getIsActive());
    return new DrawResultResponse(dn.getDraw().getId(), userDto, dn.getDrawnAt());
  }

  private ResponseEntity<Map<String, Object>> badRequest(String path, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Bad Request");
    body.put("message", message);
    body.put("timestamp", Instant.now().toString());
    body.put("path", path);
    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(body);
  }

  private ResponseEntity<Map<String, Object>> unauthorized(String path) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Unauthorized");
    body.put("message", "Invalid or missing JWT token");
    body.put("timestamp", Instant.now().toString());
    body.put("path", path);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  private ResponseEntity<Map<String, Object>> forbidden(String path, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Forbidden");
    body.put("message", message);
    body.put("timestamp", Instant.now().toString());
    body.put("path", path);
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  private ResponseEntity<Map<String, Object>> notFound(String path) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Not Found");
    body.put("message", "Resource not found");
    body.put("timestamp", Instant.now().toString());
    body.put("path", path);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  private ResponseEntity<Map<String, Object>> conflict(String path, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "Conflict");
    body.put("message", message);
    body.put("timestamp", Instant.now().toString());
    body.put("path", path);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }
}
