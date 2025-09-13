package com.namedraw.exception;

/**
 * Exception thrown when an authentication operation fails due to invalid credentials.
 *
 * <p>This exception should be used for scenarios where the user's credentials (tokens, passwords,
 * etc.) are invalid, expired, or revoked, resulting in a 401 Unauthorized HTTP response.
 */
public class UnauthorizedException extends RuntimeException {

  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
