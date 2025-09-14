package com.namedraw.dto;

import com.namedraw.model.Draw.DrawState;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Draw entity.
 *
 * <p>Represents a draw in API responses with computed fields for user permissions (canJoin,
 * canDraw) and relationships to creator and participants.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrawResponse {

  private UUID id;
  private String title;
  private String description;
  private DrawState state;
  private LocalDate drawDate;
  private Integer participantCount;
  private Integer maxParticipants;
  private UserResponse creator;
  private List<UserResponse> participants;
  private Boolean canJoin;
  private Boolean canDraw;
  private LocalDateTime createdAt;
  private LocalDateTime openedAt;
  private LocalDateTime archivedAt;
}