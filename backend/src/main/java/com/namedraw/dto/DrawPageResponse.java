package com.namedraw.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for paginated draw results.
 *
 * <p>Follows Spring Data Page structure for consistent pagination across the application.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrawPageResponse {

  private List<DrawResponse> content;
  private Long totalElements;
  private Integer totalPages;
  private Integer size;
  private Integer number;
  private Boolean first;
  private Boolean last;
}