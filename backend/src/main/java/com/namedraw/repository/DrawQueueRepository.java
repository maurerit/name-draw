package com.namedraw.repository;

import com.namedraw.model.DrawQueue;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA repository interface for DrawQueue entity operations.
 *
 * <p>This repository provides atomic data access methods for DrawQueue entities to support
 * concurrent draw management. The DrawQueue serves as an in-memory lock mechanism using primary key
 * constraints to ensure only one draw operation can occur per draw at any given time.
 *
 * <p>Key functionality includes: - Atomic insert operations for draw locking - Atomic delete
 * operations for draw unlocking - Existence checks for concurrency control - Minimal memory
 * footprint with single UUID operations
 *
 * <p>Usage Pattern: 1. Insert DrawQueue record when draw operation starts 2. Perform draw logic 3.
 * Delete DrawQueue record when operation completes (success or failure)
 *
 * <p>The primary key constraint on drawId ensures that only one DrawQueue record can exist per
 * draw, providing the atomic operation guarantee needed for safe concurrent access.
 */
@Repository
public interface DrawQueueRepository extends JpaRepository<DrawQueue, UUID> {

  /**
   * Checks if a draw operation is currently in progress for the specified draw.
   *
   * <p>This method is used to determine if a draw is currently locked by another operation. A
   * return value of true indicates that another user or process is currently performing a draw
   * operation for this draw.
   *
   * @param drawId the UUID of the draw to check
   * @return true if a draw operation is in progress (DrawQueue record exists), false otherwise
   */
  boolean existsByDrawId(UUID drawId);

  /**
   * Removes the DrawQueue lock record for the specified draw.
   *
   * <p>This method is called to release the lock after a draw operation completes, whether
   * successful or failed. It enables other participants to attempt drawing from the same draw.
   *
   * @param drawId the UUID of the draw to unlock
   * @return the number of records deleted (should be 0 or 1)
   */
  int deleteByDrawId(UUID drawId);

  @Modifying
  @Transactional
  @Query(value = "INSERT INTO draw_queue (draw_id) VALUES (:drawId)", nativeQuery = true)
  int tryLock(@Param("drawId") UUID drawId);
}
