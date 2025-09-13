package com.namedraw.controller;

import com.namedraw.exception.UnauthorizedException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for REST controllers.
 *
 * <p>This handler provides centralized exception handling for all controllers in the application.
 * It converts common exceptions to appropriate HTTP responses with standardized error format.
 *
 * <p>Error response format follows the OpenAPI ErrorResponse schema: - error: Error code/type -
 * message: Human-readable error message - timestamp: ISO-8601 timestamp when error occurred - path:
 * Request path where error occurred
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handle IllegalArgumentException and convert to 400 Bad Request.
   *
   * @param ex The IllegalArgumentException
   * @param request The web request
   * @return ResponseEntity with error details and 400 status
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {

    log.warn("Bad request: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "INVALID_ARGUMENT");
    errorResponse.put("message", ex.getMessage());
    errorResponse.put("timestamp", Instant.now().toString());
    errorResponse.put("path", request.getDescription(false).replace("uri=", ""));

    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(errorResponse);
  }

  /**
   * Handle UnauthorizedException and convert to 401 Unauthorized.
   *
   * @param ex The UnauthorizedException
   * @param request The web request
   * @return ResponseEntity with error details and 401 status
   */
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, Object>> handleUnauthorizedException(
      UnauthorizedException ex, WebRequest request) {

    log.warn("Unauthorized access: {}", ex.getMessage());

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "UNAUTHORIZED");
    errorResponse.put("message", ex.getMessage());
    errorResponse.put("timestamp", Instant.now().toString());
    errorResponse.put("path", request.getDescription(false).replace("uri=", ""));

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(errorResponse);
  }

  /**
   * Handle generic exceptions and convert to 500 Internal Server Error.
   *
   * @param ex The generic exception
   * @param request The web request
   * @return ResponseEntity with error details and 500 status
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(
      Exception ex, WebRequest request) {

    log.error("Internal server error", ex);

    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", "INTERNAL_ERROR");
    errorResponse.put("message", "An internal error occurred");
    errorResponse.put("timestamp", Instant.now().toString());
    errorResponse.put("path", request.getDescription(false).replace("uri=", ""));

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
