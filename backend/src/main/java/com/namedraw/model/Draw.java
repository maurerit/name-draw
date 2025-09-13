package com.namedraw.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Represents a name drawing event with lifecycle states.
 *
 * <p>This entity manages the entire lifecycle of a name drawing event from creation through joining
 * phase, opening for draws, and final archival. It tracks participants, enforces state transitions,
 * and maintains draw metadata.
 */
@Entity
@Table(name = "draws")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Draw {

  /**
   * Enum representing the possible states of a draw.
   *
   * <p>State transitions: - JOINING → OPEN: Manual transition by creator or automatic at 30
   * participants - OPEN → ARCHIVED: Automatic transition on draw_date - JOINING → ARCHIVED:
   * Automatic transition on draw_date (if only 1 participant)
   */
  public enum DrawState {
    JOINING,
    OPEN,
    ARCHIVED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "creator_id", nullable = false)
  private User creator;

  @NotNull
  @NotBlank
  @Size(min = 1, max = 100)
  @Column(name = "title", nullable = false)
  private String title;

  @Size(max = 500)
  @Column(name = "description")
  private String description;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Builder.Default
  @Column(name = "state", nullable = false)
  private DrawState state = DrawState.JOINING;

  @NotNull
  @Column(name = "draw_date", nullable = false)
  private LocalDate drawDate;

  @NotNull
  @Builder.Default
  @Column(name = "participant_count", nullable = false)
  private Integer participantCount = 0;

  @NotNull
  @Min(2)
  @Max(30)
  @Builder.Default
  @Column(name = "max_participants", nullable = false)
  private Integer maxParticipants = 30;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "opened_at")
  private LocalDateTime openedAt;

  @Column(name = "archived_at")
  private LocalDateTime archivedAt;

  // TODO: Uncomment when Participation entity is created (T029)
  // @OneToMany(mappedBy = "draw", fetch = FetchType.LAZY)
  // @Builder.Default
  // private List<Participation> participations = new ArrayList<>();

  // TODO: Uncomment when DrawnName entity is created (T030)
  // @OneToMany(mappedBy = "draw", fetch = FetchType.LAZY)
  // @Builder.Default
  // private List<DrawnName> drawnNames = new ArrayList<>();

  /**
   * Transitions the draw to OPEN state.
   *
   * @throws IllegalStateException if the draw is not in JOINING state
   */
  public void openForDrawing() {
    if (state != DrawState.JOINING) {
      throw new IllegalStateException("Can only open draws that are in JOINING state");
    }
    this.state = DrawState.OPEN;
    this.openedAt = LocalDateTime.now();
  }

  /**
   * Archives the draw.
   *
   * @throws IllegalStateException if the draw is already ARCHIVED
   */
  public void archive() {
    if (state == DrawState.ARCHIVED) {
      throw new IllegalStateException("Draw is already archived");
    }
    this.state = DrawState.ARCHIVED;
    this.archivedAt = LocalDateTime.now();
  }

  /**
   * Increments the participant count.
   *
   * @throws IllegalStateException if adding would exceed max participants or draw is not JOINING
   */
  public void addParticipant() {
    if (state != DrawState.JOINING) {
      throw new IllegalStateException(
          "Cannot add participants to a draw that is not in JOINING state");
    }
    if (participantCount >= maxParticipants) {
      throw new IllegalStateException("Cannot exceed maximum participant limit");
    }
    this.participantCount++;

    // Auto-transition to OPEN if we reach max participants
    if (participantCount >= maxParticipants) {
      openForDrawing();
    }
  }

  /**
   * Decrements the participant count.
   *
   * @throws IllegalStateException if count would go below zero
   */
  public void removeParticipant() {
    if (participantCount <= 0) {
      throw new IllegalStateException("Cannot remove participants when count is already zero");
    }
    this.participantCount--;
  }

  /**
   * Checks if the draw can accept new participants.
   *
   * @return true if the draw is in JOINING state and under max capacity
   */
  public boolean canJoin() {
    return state == DrawState.JOINING && participantCount < maxParticipants;
  }

  /**
   * Checks if participants can draw names.
   *
   * @return true if the draw is in OPEN state
   */
  public boolean canDraw() {
    return state == DrawState.OPEN;
  }

  /**
   * Checks if the draw date has passed and should be archived.
   *
   * @return true if the current date is after the draw date
   */
  public boolean shouldBeArchived() {
    return LocalDate.now().isAfter(drawDate);
  }

  /**
   * Checks if the draw has only one participant and should be auto-archived.
   *
   * @return true if participant count is 1 or less
   */
  public boolean shouldBeAutoArchived() {
    return participantCount <= 1;
  }
}
