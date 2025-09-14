package com.namedraw.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new draw.
 *
 * <p>Validates input according to business rules: - Title: 1-100 characters, required -
 * Description: max 500 characters, optional - Draw date: must be in the future, required - Max
 * participants: 2-30, defaults to 30 if not specified
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDrawRequest {

  @NotBlank(message = "Draw title is required")
  @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
  private String title;

  @Size(max = 500, message = "Description cannot exceed 500 characters")
  private String description;

  @Future(message = "Draw date must be in the future")
  private LocalDate drawDate;

  @Min(value = 2, message = "Minimum participants is 2")
  @Max(value = 30, message = "Maximum participants is 30")
  private Integer maxParticipants = 30;
}
