package com.namedraw.repository;

import com.namedraw.model.Draw;
import com.namedraw.model.Participation;
import com.namedraw.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository interface for Participation entity operations.
 *
 * <p>This repository provides data access methods for Participation entities with a focus on
 * user-draw relationships, participant management, and draw membership operations. All methods
 * respect business rules around draw states and participation constraints.
 *
 * <p>Key functionality includes: - User participation lookup and validation - Draw participant
 * listing and management - Participation existence checking for business logic - Bulk operations
 * for participant administration - User activity tracking across multiple draws
 */
@Repository
public interface ParticipationRepository extends JpaRepository<Participation, UUID> {

  /**
   * Finds all participations for a specific user.
   *
   * <p>This method is essential for user dashboard functionality where users need to view all draws
   * they have joined. Results are ordered by join date descending to show most recent
   * participations first.
   *
   * @param user the user whose participations to find
   * @return List of participations for the specified user, ordered by join date descending
   */
  List<Participation> findByUserOrderByJoinedAtDesc(User user);

  /**
   * Finds all participations for a specific draw.
   *
   * <p>This method is essential for draw management functionality where creators and administrators
   * need to view all participants in a draw. Results are ordered by join date ascending to show the
   * order in which users joined.
   *
   * @param draw the draw whose participants to find
   * @return List of participations for the specified draw, ordered by join date ascending
   */
  List<Participation> findByDrawOrderByJoinedAtAsc(Draw draw);

  /**
   * Finds a specific participation by user and draw.
   *
   * <p>This method is used to check if a user has already joined a specific draw, as the business
   * rules prevent users from joining the same draw multiple times. The unique constraint on
   * (user_id, draw_id) ensures at most one result.
   *
   * @param user the user to check participation for
   * @param draw the draw to check participation in
   * @return Optional containing the participation if it exists, empty otherwise
   */
  Optional<Participation> findByUserAndDraw(User user, Draw draw);

  /**
   * Checks if a user has joined a specific draw.
   *
   * <p>This is a convenience method for business logic that needs to quickly verify participation
   * status without retrieving the full Participation entity. More efficient than using {@link
   * #findByUserAndDraw(User, Draw)} when only existence matters.
   *
   * @param user the user to check participation for
   * @param draw the draw to check participation in
   * @return true if the user has joined the draw, false otherwise
   */
  boolean existsByUserAndDraw(User user, Draw draw);

  /**
   * Counts the number of participants in a specific draw.
   *
   * <p>This method provides an efficient way to get participant counts without loading all
   * Participation entities. Used for capacity checking and draw management dashboard displays.
   *
   * @param draw the draw to count participants for
   * @return the number of participants in the draw
   */
  long countByDraw(Draw draw);

  /**
   * Finds all participations for draws created by a specific user.
   *
   * <p>This method allows draw creators to see all participants across all their draws. Useful for
   * creator dashboard views and participant management across multiple draws.
   *
   * @param creator the user who created the draws
   * @return List of participations in draws created by the specified user
   */
  @Query("SELECT p FROM Participation p WHERE p.draw.creator = :creator ORDER BY p.joinedAt DESC")
  List<Participation> findByDrawCreator(@Param("creator") User creator);

  /**
   * Finds all users who have joined a specific draw.
   *
   * <p>This method provides direct access to User entities who are participants in a draw, without
   * needing to navigate through Participation entities. Useful for business logic that operates on
   * participant lists.
   *
   * @param draw the draw whose participants to find
   * @return List of users who have joined the specified draw, ordered by join date ascending
   */
  @Query("SELECT p.user FROM Participation p WHERE p.draw = :draw ORDER BY p.joinedAt ASC")
  List<User> findUsersByDraw(@Param("draw") Draw draw);

  /**
   * Finds all draws that a specific user has joined.
   *
   * <p>This method provides direct access to Draw entities that a user has joined, without needing
   * to navigate through Participation entities. Useful for business logic that operates on user's
   * draw lists.
   *
   * @param user the user whose joined draws to find
   * @return List of draws that the specified user has joined, ordered by join date descending
   */
  @Query("SELECT p.draw FROM Participation p WHERE p.user = :user ORDER BY p.joinedAt DESC")
  List<Draw> findDrawsByUser(@Param("user") User user);

  /**
   * Finds all participations for active users in a specific draw.
   *
   * <p>This method filters out participations from deactivated users, which is important for draw
   * validation and ensuring only active accounts can participate in drawings. Used during draw
   * state transitions and drawing operations.
   *
   * @param draw the draw whose active participants to find
   * @return List of participations for active users in the specified draw
   */
  @Query(
      """
    SELECT p
      FROM Participation p
     WHERE p.draw = :draw
       AND p.user.isActive = true
     ORDER BY p.joinedAt ASC
      """)
  List<Participation> findActiveParticipationsByDraw(@Param("draw") Draw draw);

  /**
   * Counts the number of active participants in a specific draw.
   *
   * <p>This method provides an efficient way to get active participant counts, excluding
   * deactivated users. Used for capacity checking and draw validation when transitioning between
   * states.
   *
   * @param draw the draw to count active participants for
   * @return the number of active participants in the draw
   */
  @Query(
      """
    SELECT COUNT(p)
      FROM Participation p
     WHERE p.draw = :draw
       AND p.user.isActive = true
      """)
  long countActiveParticipationsByDraw(@Param("draw") Draw draw);

  /**
   * Removes all participations for deactivated users from a specific draw.
   *
   * <p>This method is used during draw state transitions to clean up participations from users
   * whose accounts have been deactivated. This ensures only active users can participate in drawing
   * operations.
   *
   * @param draw the draw to clean up participations for
   * @return the number of participations removed
   */
  @Modifying
  @Query("DELETE FROM Participation p WHERE p.draw = :draw AND p.user.isActive = false")
  int removeInactiveParticipationsByDraw(@Param("draw") Draw draw);

  /**
   * Finds all participations for a user in draws with a specific state.
   *
   * <p>This method allows filtering a user's participations by the current state of the draws.
   * Useful for showing users only their active draws, archived draws, or draws open for drawing.
   *
   * @param user the user whose participations to find
   * @param drawState the state of draws to filter by
   * @return List of participations for the user in draws with the specified state
   */
  @Query(
      """
    SELECT p
      FROM Participation p
     WHERE p.user = :user
       AND p.draw.state = :drawState
     ORDER BY p.joinedAt DESC
      """)
  List<Participation> findByUserAndDrawState(
      @Param("user") User user, @Param("drawState") Draw.DrawState drawState);

  /**
   * Finds all participations for draws with a specific state created by a specific user.
   *
   * <p>This method allows draw creators to see participants only in their draws that have a
   * specific state. Useful for creator dashboards that need to show participants in active vs
   * archived draws.
   *
   * @param creator the user who created the draws
   * @param drawState the state of draws to filter by
   * @return List of participations in draws created by the user with the specified state
   */
  @Query(
      """
    SELECT p
      FROM Participation p
     WHERE p.draw.creator = :creator
       AND p.draw.state = :drawState
     ORDER BY p.joinedAt DESC
      """)
  List<Participation> findByDrawCreatorAndDrawState(
      @Param("creator") User creator, @Param("drawState") Draw.DrawState drawState);
}
