package com.namedraw.repository;

import com.namedraw.model.Draw;
import com.namedraw.model.DrawnName;
import com.namedraw.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository interface for DrawnName entity operations.
 *
 * <p>This repository provides data access methods for DrawnName entities with a focus on draw
 * result management, participant draw tracking, and result retrieval. All methods enforce business
 * rules around unique draws per participant and unique name assignments per draw.
 *
 * <p>Key functionality includes: - Individual draw result lookup for authenticated users - Bulk
 * draw result retrieval for draw administrators - Draw completion status checking and validation -
 * Participant draw history and audit trails - Name assignment uniqueness enforcement
 */
@Repository
public interface DrawnNameRepository extends JpaRepository<DrawnName, UUID> {

  /**
   * Finds the draw result for a specific user in a specific draw.
   *
   * <p>This method is used to retrieve what name a particular user drew from a specific draw.
   * Essential for the "my result" API endpoint and personal draw history views.
   *
   * @param draw the draw to check
   * @param drawerUser the user who performed the draw
   * @return Optional containing the drawn name result, or empty if user hasn't drawn yet
   */
  Optional<DrawnName> findByDrawAndDrawerUser(Draw draw, User drawerUser);

  /**
   * Finds all draw results for a specific draw.
   *
   * <p>This method retrieves all completed draws for a specific draw event, ordered by when each
   * draw was performed. Used for draw results pages and administrative views.
   *
   * @param draw the draw to get results for
   * @return List of all draw results for the specified draw, ordered by draw time
   */
  List<DrawnName> findByDrawOrderByDrawnAtAsc(Draw draw);

  /**
   * Checks if a specific user has already drawn a name in a specific draw.
   *
   * <p>This method provides efficient existence checking to prevent multiple draws by the same
   * user. Used for validation before allowing draw operations and UI state management.
   *
   * @param draw the draw to check
   * @param drawerUser the user to check for existing draws
   * @return true if the user has already drawn from this draw, false otherwise
   */
  boolean existsByDrawAndDrawerUser(Draw draw, User drawerUser);

  /**
   * Checks if a specific user's name has already been drawn in a specific draw.
   *
   * <p>This method ensures each participant's name can only be drawn once per draw. Used for
   * validation during draw operations and for determining available names to draw.
   *
   * @param draw the draw to check
   * @param drawnUser the user whose name to check
   * @return true if this user's name has already been drawn, false otherwise
   */
  boolean existsByDrawAndDrawnUser(Draw draw, User drawnUser);

  /**
   * Counts the number of completed draws for a specific draw.
   *
   * <p>This method provides an efficient way to check draw completion status without loading all
   * DrawnName entities. Used for progress tracking and determining when draws are complete.
   *
   * @param draw the draw to count completed draws for
   * @return the number of participants who have completed their draws
   */
  long countByDraw(Draw draw);

  /**
   * Finds all draws performed by a specific user across all draws they've participated in.
   *
   * <p>This method allows users to see their complete draw history across all draws. Useful for
   * user profile pages and personal draw history views.
   *
   * @param drawerUser the user to get draw history for
   * @return List of all draws performed by the user, ordered by most recent first
   */
  List<DrawnName> findByDrawerUserOrderByDrawnAtDesc(User drawerUser);

  /**
   * Finds all instances where a specific user's name was drawn across all draws.
   *
   * <p>This method shows where a user's name has been selected by others. Useful for analytics and
   * "who drew me" type queries.
   *
   * @param drawnUser the user whose name selections to find
   * @return List of all times this user's name was drawn, ordered by most recent first
   */
  List<DrawnName> findByDrawnUserOrderByDrawnAtDesc(User drawnUser);

  /**
   * Finds all draw results for draws created by a specific user.
   *
   * <p>This method allows draw creators to see all draw results across all their created draws.
   * Useful for creator dashboards and comprehensive draw management views.
   *
   * @param creator the user who created the draws
   * @return List of all draw results from draws created by the specified user
   */
  @Query("SELECT dn FROM DrawnName dn WHERE dn.draw.creator = :creator ORDER BY dn.drawnAt DESC")
  List<DrawnName> findByDrawCreator(@Param("creator") User creator);

  /**
   * Finds draw results for a specific draw within a date range.
   *
   * <p>This method supports analytics and reporting by allowing filtered result retrieval based on
   * when draws were performed. Useful for time-based analysis and audit reports.
   *
   * @param draw the draw to filter results for
   * @param startDate the earliest draw time to include (inclusive)
   * @param endDate the latest draw time to include (inclusive)
   * @return List of draw results within the specified time range
   */
  List<DrawnName> findByDrawAndDrawnAtBetweenOrderByDrawnAtAsc(
      Draw draw, LocalDateTime startDate, LocalDateTime endDate);

  /**
   * Gets summary statistics for a specific draw showing completion progress.
   *
   * <p>This query provides efficient aggregated data about draw completion without loading all
   * entities. Returns the total number of draws completed for reporting and progress tracking.
   *
   * @param draw the draw to get statistics for
   * @return count of completed draws, total participants can be obtained from Participation
   *     repository
   */
  @Query("SELECT COUNT(dn) FROM DrawnName dn WHERE dn.draw = :draw")
  long getDrawCompletionCount(@Param("draw") Draw draw);

  /**
   * Finds users who have not yet drawn names in a specific draw.
   *
   * <p>This method identifies participants who still need to draw names by finding users who have
   * joined the draw but don't have corresponding DrawnName records. Essential for draw progress
   * tracking and reminder notifications.
   *
   * @param draw the draw to check for pending draws
   * @return List of users who have joined but not yet drawn from the specified draw
   */
  @Query(
      """
    SELECT p.user
      FROM Participation p
     WHERE p.draw = :draw
       AND NOT EXISTS (SELECT 1
                         FROM DrawnName dn
                        WHERE dn.draw = :draw
                          AND dn.drawerUser = p.user)
      """)
  List<User> findUsersWhoHaveNotDrawn(@Param("draw") Draw draw);

  /**
   * Finds available users whose names can still be drawn in a specific draw.
   *
   * <p>This method identifies participants whose names are still available to be drawn by finding
   * users who have joined the draw but whose names haven't been selected yet. Essential for the
   * draw operation to know which names are available.
   *
   * @param draw the draw to check for available names
   * @return List of users whose names are still available to be drawn
   */
  @Query(
      """
    SELECT p.user
      FROM Participation p
     WHERE p.draw = :draw
       AND NOT EXISTS (SELECT 1
                         FROM DrawnName dn
                        WHERE dn.draw = :draw
                          AND dn.drawnUser = p.user)
      """)
  List<User> findAvailableUsersToDraw(@Param("draw") Draw draw);
}
