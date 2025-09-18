package com.namedraw.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single draw result.
 *
 * <p>Represents the outcome of a drawing operation including the draw identifier, the user who was
 * drawn, and the timestamp of when the draw occurred.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrawResultResponse {

  private UUID drawId;
  private UserDetailResponse drawnUser;
  private LocalDateTime drawnAt;

  /** Nested user details including email for draw result responses. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserDetailResponse {
    private UUID id;
    private String name;
    private String email;
    private String profilePictureUrl;
    private Boolean isActive;
  }
}
