package com.namedraw.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks participants eligible to draw names (in-memory for performance).
 *
 * <p>This entity provides atomic operations for concurrent draw management by utilizing a primary
 * key constraint to ensure only one person can draw at a time. The minimal memory footprint (single
 * UUID per active draw operation) makes it ideal for high-performance transactional operations.
 *
 * <p>Lifecycle: - Record inserted when user clicks draw button - Record deleted when draw operation
 * completes (success or failure) - Table remains empty when no draws are in progress
 *
 * <p>The primary key constraint on draw_id ensures atomic operation: only one draw can be in
 * progress per draw at any given time.
 */
@Entity
@Table(name = "draw_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawQueue {

  /**
   * Draw identifier - serves as both primary key and foreign key to Draw table.
   *
   * <p>The primary key constraint ensures only one record per draw_id can exist, providing the
   * atomic operation guarantee needed for concurrent draw management.
   */
  @Id
  @NotNull
  @Column(name = "draw_id", nullable = false)
  private UUID drawId;
}
