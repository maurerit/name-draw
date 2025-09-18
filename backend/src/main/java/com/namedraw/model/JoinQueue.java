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
 * Lightweight in-memory lock for serializing join operations per draw.
 *
 * <p>Acts like a mutex keyed by draw_id using a primary key constraint so that only one concurrent
 * join attempt can proceed at a time for the same draw.
 */
@Entity
@Table(name = "join_queue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinQueue {

  @Id
  @NotNull
  @Column(name = "draw_id", nullable = false)
  private UUID drawId;
}
