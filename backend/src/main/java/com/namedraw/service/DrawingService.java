package com.namedraw.service;

import com.namedraw.model.Draw;
import com.namedraw.model.Draw.DrawState;
import com.namedraw.model.DrawQueue;
import com.namedraw.model.DrawnName;
import com.namedraw.model.Participation;
import com.namedraw.model.User;
import com.namedraw.repository.DrawQueueRepository;
import com.namedraw.repository.DrawnNameRepository;
import com.namedraw.repository.ParticipationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for drawing operations with queue management.
 *
 * <p>This service provides business logic for name drawing operations including queue-based
 * concurrency control, self-draw prevention, and draw result management. All operations respect the
 * draw state machine and business rules defined in the specification.
 *
 * <p>Key responsibilities: - Name drawing with automatic self-draw prevention - Queue-based
 * concurrency control (one draw at a time per draw) - Draw result retrieval with proper access
 * controls - Transaction management for atomic operations - Business rule enforcement (draw state,
 * participant eligibility, etc.)
 */
@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)
public class DrawingService {

  private final DrawQueueRepository drawQueueRepository;
  private final DrawnNameRepository drawnNameRepository;
  private final ParticipationRepository participationRepository;
  private final DrawService drawService;
  private final Random random = new Random();

  /**
   * Performs a name draw for the specified user in the specified draw.
   *
   * <p>This method implements queue-based concurrency control using the DrawQueue entity to ensure
   * only one participant can draw at a time from the same draw. It also handles self-draw
   * prevention by automatically redrawing until a different participant is selected.
   *
   * <p>Algorithm: 1. Insert DrawQueue record for atomic locking 2. Validate draw state and user
   * eligibility 3. Select random participant (with self-draw prevention) 4. Create DrawnName record
   * 5. Remove DrawQueue record to release lock
   *
   * @param drawId the ID of the draw to perform drawing from
   * @param drawerUser the authenticated user performing the draw
   * @return DrawnName the result of the draw operation
   * @throws IllegalStateException if draw is not in OPEN state
   * @throws IllegalArgumentException if user is not a participant or has already drawn
   * @throws RuntimeException if another draw is already in progress (conflict)
   * @throws RuntimeException if unable to find a non-self name after maximum attempts
   */
  @Transactional
  public DrawnName performDraw(UUID drawId, User drawerUser) {
    log.info("Starting draw operation for user {} in draw {}", drawerUser.getId(), drawId);

    // Step 1: Acquire draw lock using queue mechanism
    DrawQueue queueEntry = DrawQueue.builder().drawId(drawId).build();
    try {
      drawQueueRepository.save(queueEntry);
      log.debug("Successfully acquired draw lock for draw {}", drawId);
    } catch (DataIntegrityViolationException e) {
      log.warn("Draw already in progress for draw {}", drawId);
      throw new RuntimeException("Another draw is already in progress. Please try again.", e);
    }

    try {
      // Step 2: Validate draw exists and get it
      Optional<Draw> drawOpt = drawService.findDrawById(drawId);
      if (drawOpt.isEmpty()) {
        log.warn("Draw not found: {}", drawId);
        throw new IllegalArgumentException("Draw not found");
      }
      Draw draw = drawOpt.get();

      // Step 3: Validate draw state
      if (draw.getState() != DrawState.OPEN) {
        log.warn("Attempted to draw from draw {} in invalid state: {}", drawId, draw.getState());
        throw new IllegalStateException("Draw is not open for drawing");
      }

      // Step 4: Validate user is a participant
      Optional<Participation> participation =
          participationRepository.findByUserAndDraw(drawerUser, draw);
      if (participation.isEmpty()) {
        log.warn("User {} is not a participant in draw {}", drawerUser.getId(), drawId);
        throw new IllegalArgumentException("User is not a participant in this draw");
      }

      // Step 5: Check if user has already drawn
      Optional<DrawnName> existingDraw =
          drawnNameRepository.findByDrawAndDrawerUser(draw, drawerUser);
      if (existingDraw.isPresent()) {
        log.warn("User {} has already drawn in draw {}", drawerUser.getId(), drawId);
        throw new IllegalArgumentException("User has already drawn from this draw");
      }

      // Step 6: Get all participants
      List<Participation> allParticipants =
          participationRepository.findByDrawOrderByJoinedAtAsc(draw);
      if (allParticipants.size() <= 1) {
        log.warn(
            "Cannot perform draw with {} participants in draw {}", allParticipants.size(), drawId);
        throw new IllegalStateException("Cannot perform draw with insufficient participants");
      }

      // Step 7: Get all already drawn participants to exclude them
      List<DrawnName> alreadyDrawnNames = drawnNameRepository.findByDrawOrderByDrawnAtAsc(draw);
      List<UUID> alreadyDrawnUserIds =
          alreadyDrawnNames.stream().map(drawnName -> drawnName.getDrawnUser().getId()).toList();

      // Step 8: Find eligible participants (exclude drawer and already drawn)
      List<User> eligibleUsers =
          allParticipants.stream()
              .map(Participation::getUser)
              .filter(user -> !user.getId().equals(drawerUser.getId())) // Exclude self
              .filter(user -> !alreadyDrawnUserIds.contains(user.getId())) // Exclude already drawn
              .toList();

      if (eligibleUsers.isEmpty()) {
        log.warn("No eligible users to draw for user {} in draw {}", drawerUser.getId(), drawId);
        throw new RuntimeException("No eligible participants available to draw");
      }

      // Step 9: Select random eligible user
      User drawnUser = eligibleUsers.get(random.nextInt(eligibleUsers.size()));
      log.info("User {} drew user {} in draw {}", drawerUser.getId(), drawnUser.getId(), drawId);

      // Step 10: Create DrawnName record
      DrawnName drawnName =
          DrawnName.builder()
              .draw(draw)
              .drawerUser(drawerUser)
              .drawnUser(drawnUser)
              .drawnAt(LocalDateTime.now())
              .build();

      DrawnName savedDrawnName = drawnNameRepository.save(drawnName);
      log.info(
          "Successfully completed draw operation for user {} in draw {}",
          drawerUser.getId(),
          drawId);

      return savedDrawnName;

    } finally {
      // Step 11: Always release the draw lock
      drawQueueRepository.delete(queueEntry);
      log.debug("Released draw lock for draw {}", drawId);
    }
  }

  /**
   * Retrieves all draw results for the specified draw.
   *
   * <p>This method returns all completed draws for a specific draw event, ordered by when each draw
   * was performed. Access control is handled at the controller level.
   *
   * @param drawId the ID of the draw to get results for
   * @return List of all draw results for the specified draw, ordered by draw time
   * @throws IllegalArgumentException if draw does not exist
   */
  public List<DrawnName> getDrawResults(UUID drawId) {
    log.debug("Retrieving draw results for draw {}", drawId);

    Optional<Draw> drawOpt = drawService.findDrawById(drawId);
    if (drawOpt.isEmpty()) {
      throw new IllegalArgumentException("Draw not found");
    }
    Draw draw = drawOpt.get();

    List<DrawnName> results = drawnNameRepository.findByDrawOrderByDrawnAtAsc(draw);

    log.debug("Found {} draw results for draw {}", results.size(), drawId);
    return results;
  }

  /**
   * Retrieves the draw result for the specified user in the specified draw.
   *
   * <p>This method returns what name a particular user drew from a specific draw. Returns empty
   * Optional if the user hasn't drawn yet.
   *
   * @param drawId the ID of the draw to check
   * @param user the user to get the draw result for
   * @return Optional containing the drawn name result, or empty if user hasn't drawn yet
   * @throws IllegalArgumentException if draw does not exist
   */
  public Optional<DrawnName> getMyDrawResult(UUID drawId, User user) {
    log.debug("Retrieving draw result for user {} in draw {}", user.getId(), drawId);

    Optional<Draw> drawOpt = drawService.findDrawById(drawId);
    if (drawOpt.isEmpty()) {
      throw new IllegalArgumentException("Draw not found");
    }
    Draw draw = drawOpt.get();

    Optional<DrawnName> result = drawnNameRepository.findByDrawAndDrawerUser(draw, user);

    if (result.isPresent()) {
      log.debug("Found draw result for user {} in draw {}", user.getId(), drawId);
    } else {
      log.debug("No draw result found for user {} in draw {}", user.getId(), drawId);
    }

    return result;
  }

  /**
   * Checks if a user has already drawn from the specified draw.
   *
   * <p>This is a convenience method to check drawing eligibility without retrieving the full
   * result.
   *
   * @param drawId the ID of the draw to check
   * @param user the user to check
   * @return true if the user has already drawn, false otherwise
   */
  public boolean hasUserDrawn(UUID drawId, User user) {
    return getMyDrawResult(drawId, user).isPresent();
  }

  /**
   * Checks if a draw operation is currently in progress for the specified draw.
   *
   * <p>This method checks the DrawQueue to see if another user is currently performing a draw
   * operation.
   *
   * @param drawId the ID of the draw to check
   * @return true if a draw is in progress, false otherwise
   */
  public boolean isDrawInProgress(UUID drawId) {
    return drawQueueRepository.existsById(drawId);
  }
}
