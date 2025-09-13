package com.namedraw.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Records which participant drew which name.
 *
 * <p>This entity captures the results of name drawing operations, tracking who drew whose name and
 * when the draw occurred. It enforces business rules to prevent duplicate draws and self-draws.
 */
@Entity
@Table(
    name = "drawn_names",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"draw_id", "drawer_user_id"}),
      @UniqueConstraint(columnNames = {"draw_id", "drawn_user_id"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrawnName {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "draw_id", nullable = false)
  private Draw draw;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "drawer_user_id", nullable = false)
  private User drawerUser;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "drawn_user_id", nullable = false)
  private User drawnUser;

  @NotNull
  @CreationTimestamp
  @Column(name = "drawn_at", nullable = false, updatable = false)
  private LocalDateTime drawnAt;
}
