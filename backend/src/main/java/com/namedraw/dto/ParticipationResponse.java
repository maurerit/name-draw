package com.namedraw.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Participation entity.
 *
 * <p>Represents a user's participation in a draw, including when they joined.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationResponse {

  private UUID id;
  private DrawResponse draw;
  private LocalDateTime joinedAt;
}