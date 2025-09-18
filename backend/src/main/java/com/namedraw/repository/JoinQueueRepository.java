package com.namedraw.repository;

import com.namedraw.model.JoinQueue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Repository for join-queue locks to serialize concurrent join operations. */
@Repository
public interface JoinQueueRepository extends JpaRepository<JoinQueue, UUID> {
  boolean existsByDrawId(UUID drawId);

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  int deleteByDrawId(UUID drawId);

  @Modifying
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Query(value = "INSERT INTO join_queue (draw_id) VALUES (:drawId)", nativeQuery = true)
  int tryLock(@Param("drawId") UUID drawId);
}
