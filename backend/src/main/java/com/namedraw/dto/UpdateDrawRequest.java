package com.namedraw.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing draw.
 *
 * <p>Allows updating draw properties when the draw is in JOINING state. Only the creator can update
 * a draw. All fields are optional to support partial updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDrawRequest {

  @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
  private String title;

  @Size(max = 500, message = "Description cannot exceed 500 characters")
  private String description;

  @Future(message = "Draw date must be in the future")
  private LocalDate drawDate;

  @Min(value = 2, message = "Minimum participants is 2")
  @Max(value = 30, message = "Maximum participants is 30")
  private Integer maxParticipants;
}