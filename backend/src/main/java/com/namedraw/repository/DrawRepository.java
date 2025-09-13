package com.namedraw.repository;

import com.namedraw.model.Draw;
import com.namedraw.model.Draw.DrawState;
import com.namedraw.model.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository interface for Draw entity operations.
 *
 * <p>This repository provides data access methods for Draw entities with a focus on state
 * management, creator relationships, and lifecycle operations. All methods respect draw state
 * transitions and business rules defined in the Draw entity.
 *
 * <p>Key functionality includes: - State-based draw filtering and management - Creator-based draw
 * access and listing - Date-based archival and lifecycle operations - Participant capacity and
 * state transition support - Bulk operations for draw administration
 */
@Repository
public interface DrawRepository extends JpaRepository<Draw, UUID> {

  /**
   * Finds all draws created by a specific user.
   *
   * <p>This method is essential for user dashboard functionality where creators need to view and
   * manage their own draws. Results are ordered by creation date descending to show newest draws
   * first.
   *
   * @param creator the user who created the draws
   * @return List of draws created by the specified user, ordered by creation date descending
   */
  List<Draw> findByCreatorOrderByCreatedAtDesc(User creator);

  /**
   * Finds all draws created by a specific user with a specific state.
   *
   * <p>Useful for filtering user's draws by lifecycle state, such as showing only active draws
   * that can still accept participants or only archived draws for historical viewing.
   *
   * @param creator the user who created the draws
   * @param state the current state of the draws
   * @return List of draws matching creator and state, ordered by creation date descending
   */
  List<Draw> findByCreatorAndStateOrderByCreatedAtDesc(User creator, DrawState state);

  /**
   * Finds all draws in a specific state.
   *
   * <p>Primary method for administrative operations and background processing. For example, finding
   * all JOINING draws to check for auto-transition conditions or all OPEN draws for archival
   * processing.
   *
   * @param state the current state of the draws
   * @return List of draws in the specified state
   */
  List<Draw> findByState(DrawState state);

  /**
   * Finds all draws that should be archived based on their draw date.
   *
   * <p>This method supports the automatic archival process by finding draws whose draw date has
   * passed but are not yet in ARCHIVED state. Used by scheduled background tasks.
   *
   * @param currentDate the current date for comparison
   * @return List of draws that should be archived
   */
  @Query("SELECT d FROM Draw d WHERE d.drawDate < :currentDate AND d.state != 'ARCHIVED'")
  List<Draw> findDrawsToArchive(@Param("currentDate") LocalDate currentDate);

  /**
   * Finds draws that should be auto-archived due to insufficient participants.
   *
   * <p>Identifies draws with only one participant that have reached their draw date, as these
   * cannot proceed with name drawing and should be automatically archived.
   *
   * @param currentDate the current date for comparison
   * @return List of draws with insufficient participants that should be archived
   */
  @Query(
      "SELECT d FROM Draw d WHERE d.drawDate <= :currentDate AND d.participantCount <= 1 AND d.state = 'JOINING'")
  List<Draw> findDrawsToAutoArchive(@Param("currentDate") LocalDate currentDate);

  /**
   * Finds draws that are at maximum capacity and should be auto-opened.
   *
   * <p>Identifies draws in JOINING state that have reached their maximum participant count and
   * should be automatically transitioned to OPEN state.
   *
   * @return List of draws at capacity that should be auto-opened
   */
  @Query("SELECT d FROM Draw d WHERE d.participantCount >= d.maxParticipants AND d.state = 'JOINING'")
  List<Draw> findDrawsToAutoOpen();

  /**
   * Finds all publicly joinable draws.
   *
   * <p>Returns draws that are in JOINING state and have capacity for additional participants. This
   * supports the public draw discovery feature where users can browse and join available draws.
   *
   * @return List of draws that can accept new participants, ordered by creation date descending
   */
  @Query("SELECT d FROM Draw d WHERE d.state = 'JOINING' AND d.participantCount < d.maxParticipants ORDER BY d.createdAt DESC")
  List<Draw> findJoinableDraws();

  /**
   * Finds draws that a specific user has not yet joined.
   *
   * <p>Supports the draw discovery feature by showing users only draws they can potentially join.
   * Excludes draws where the user is already a participant or is the creator.
   *
   * @param userId the ID of the user looking for draws to join
   * @return List of joinable draws excluding those the user created or already joined
   */
  @Query(
      "SELECT d FROM Draw d WHERE d.state = 'JOINING' AND d.participantCount < d.maxParticipants "
          + "AND d.creator.id != :userId AND d.id NOT IN "
          + "(SELECT p.draw.id FROM Participation p WHERE p.user.id = :userId) "
          + "ORDER BY d.createdAt DESC")
  List<Draw> findJoinableDrawsForUser(@Param("userId") UUID userId);

  /**
   * Finds draws created within a specific date range.
   *
   * <p>Useful for reporting and analytics, allowing administrators to analyze draw creation
   * patterns over time periods.
   *
   * @param startDate the beginning of the date range (inclusive)
   * @param endDate the end of the date range (inclusive)
   * @return List of draws created within the specified date range
   */
  List<Draw> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Finds draws with draw dates in a specific range.
   *
   * <p>Supports calendar-style views and scheduling functionality where users want to see draws
   * planned for specific time periods.
   *
   * @param startDate the beginning of the date range (inclusive)
   * @param endDate the end of the date range (inclusive)
   * @return List of draws with draw dates in the specified range
   */
  List<Draw> findByDrawDateBetween(LocalDate startDate, LocalDate endDate);

  /**
   * Counts draws created by a specific user.
   *
   * <p>Provides efficient count without loading draw entities. Useful for user statistics and
   * dashboard summary information.
   *
   * @param creator the user who created the draws
   * @return the number of draws created by the specified user
   */
  long countByCreator(User creator);

  /**
   * Counts draws created by a specific user in a specific state.
   *
   * <p>More specific counting for user dashboard statistics, such as showing how many active draws
   * a user has or how many completed draws they've organized.
   *
   * @param creator the user who created the draws
   * @param state the current state of the draws
   * @return the number of draws matching creator and state
   */
  long countByCreatorAndState(User creator, DrawState state);

  /**
   * Counts draws in a specific state.
   *
   * <p>System-wide statistics for administrative dashboards and monitoring.
   *
   * @param state the current state of the draws
   * @return the number of draws in the specified state
   */
  long countByState(DrawState state);

  /**
   * Checks if a specific user has created any draws.
   *
   * <p>Efficient existence check without loading entities. Useful for user onboarding flows or
   * feature discovery prompts.
   *
   * @param creator the user to check
   * @return true if the user has created any draws, false otherwise
   */
  boolean existsByCreator(User creator);

  /**
   * Checks if a draw with a specific title exists for a user.
   *
   * <p>Prevents users from creating duplicate draw titles, improving organization and user
   * experience.
   *
   * @param creator the user who would create the draw
   * @param title the proposed title for the draw
   * @return true if a draw with this title already exists for this user, false otherwise
   */
  boolean existsByCreatorAndTitle(User creator, String title);

  /**
   * Bulk update draw states.
   *
   * <p>Efficient bulk operation for background processing tasks such as automated archival or
   * state transitions. Avoids loading entities for better performance.
   *
   * @param drawIds the UUIDs of draws to update
   * @param newState the new state to set
   * @param timestamp the timestamp to record the state change
   * @return the number of draws updated
   */
  @Modifying
  @Query("UPDATE Draw d SET d.state = :newState, d.archivedAt = :timestamp WHERE d.id IN :drawIds AND d.state != 'ARCHIVED'")
  int bulkUpdateToArchived(
      @Param("drawIds") List<UUID> drawIds,
      @Param("newState") DrawState newState,
      @Param("timestamp") LocalDateTime timestamp);

  /**
   * Bulk update draws to OPEN state.
   *
   * <p>Efficient bulk operation for auto-opening draws that have reached capacity.
   *
   * @param drawIds the UUIDs of draws to update
   * @param timestamp the timestamp to record when draws were opened
   * @return the number of draws updated
   */
  @Modifying
  @Query("UPDATE Draw d SET d.state = 'OPEN', d.openedAt = :timestamp WHERE d.id IN :drawIds AND d.state = 'JOINING'")
  int bulkUpdateToOpen(
      @Param("drawIds") List<UUID> drawIds, @Param("timestamp") LocalDateTime timestamp);

  /**
   * Bulk update participant counts for draws.
   *
   * <p>Allows efficient synchronization of participant counts when batch processing participation
   * changes or data correction operations.
   *
   * @param drawId the UUID of the draw to update
   * @param participantCount the new participant count
   * @return the number of draws updated (should be 1 if successful)
   */
  @Modifying
  @Query("UPDATE Draw d SET d.participantCount = :participantCount WHERE d.id = :drawId")
  int updateParticipantCount(
      @Param("drawId") UUID drawId, @Param("participantCount") Integer participantCount);

  /**
   * Finds draws that need participant count synchronization.
   *
   * <p>Identifies draws where the cached participant count may be out of sync with actual
   * participation records. Used for data integrity maintenance.
   *
   * @return List of draws that may need participant count updates
   */
  @Query(
      "SELECT d FROM Draw d WHERE d.participantCount != (SELECT COUNT(p) FROM Participation p WHERE p.draw = d)")
  List<Draw> findDrawsWithIncorrectParticipantCount();
}