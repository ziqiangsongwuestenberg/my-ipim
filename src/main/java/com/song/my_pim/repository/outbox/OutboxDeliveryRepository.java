package com.song.my_pim.repository.outbox;

import com.song.my_pim.entity.outbox.OutboxDeliveryEntity;
import com.song.my_pim.entity.outbox.OutboxDeliveryStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface  OutboxDeliveryRepository extends JpaRepository<OutboxDeliveryEntity, Long> {

    long countByTarget_IdAndStatus( Long targetId, OutboxDeliveryStatus status);

    List<OutboxDeliveryEntity> findTop100ByTarget_IdAndStatusOrderByIdAsc(Long targetId, OutboxDeliveryStatus status);


    @Query("""
        select d
        from OutboxDeliveryEntity d
        join fetch d.outboxEvent ev
        join fetch d.target t
               where d.status in :statuses
                 and d.target.enabled = true
                 and (d.nextRetryAt is null or d.nextRetryAt <= :now)
                 and t.enabled = true
               order by d.creationTime asc
           """)
    List<OutboxDeliveryEntity> findDueDeliveries(@Param("statuses") List<OutboxDeliveryStatus> statuses,
                                                 @Param("now") OffsetDateTime now,
                                                 Pageable pageable);

    @Query("""
        select d from OutboxDeliveryEntity d
        join fetch d.outboxEvent e
        join fetch d.target t
        where d.id in :ids
           """)
    List<OutboxDeliveryEntity> findAllByIds(@Param("ids") List<Long> ids);

    @Query(value = """
        WITH cte AS (
          SELECT d.id
          FROM outbox_delivery d
          JOIN delivery_target t ON t.id = d.target_id
          WHERE d.target_id = :targetId
            AND t.enabled = true
            AND (
              d.status IN ('NEW','FAILED')
              OR (d.status = 'PROCESSING' AND d.claimed_until IS NOT NULL AND d.claimed_until < now()) -- last time failed
            )
            AND (d.next_retry_at IS NULL OR d.next_retry_at <= now())
          ORDER BY d.id
          FOR UPDATE SKIP LOCKED
          LIMIT :limit
        )
        UPDATE outbox_delivery d
        SET status = 'PROCESSING',
            claimed_by = :user,
            claimed_until = now() + (:leaseSeconds || ' seconds')::interval,
            attempt_count = d.attempt_count + 1,
            update_time = now(),
            update_user = :user
        FROM cte
        WHERE d.id IN (SELECT id FROM cte)
        RETURNING d.id
            """, nativeQuery = true)
    List<Long> claimDueDeliveryIds(
            @Param("targetId") long targetId,
            @Param("user") String user,
            @Param("leaseSeconds") int leaseSeconds,
            @Param("limit") int limit
    );


    /**
     * Claim a batch of deliveries for a target (pull mode).
     * Returns claimed delivery IDs.
     *
     * IMPORTANT: call inside @Transactional.
     */
    @Query(value = """
        WITH cte AS (
            SELECT d.id
            FROM outbox_delivery d
            WHERE d.target_id = :targetId
              AND (
                    d.status = 'NEW'
                    OR (d.status = 'CLAIMED' AND d.claimed_until IS NOT NULL AND d.claimed_until < now())
                  )
              AND (d.next_retry_at IS NULL OR d.next_retry_at <= now())
            ORDER BY d.id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
        )
        UPDATE outbox_delivery d
        SET status = 'CLAIMED',
            claimed_by = :consumerId,
            claimed_until = now() + (:leaseSeconds || ' seconds')::interval,
            attempt_count = d.attempt_count + 1,
            update_time = now(),
            update_user = :consumerId
        FROM cte
        WHERE d.id = cte.id
        RETURNING d.id
        """, nativeQuery = true)
    List<Long> claimBatch(
            @Param("targetId") long targetId,
            @Param("consumerId") String consumerId,
            @Param("leaseSeconds") int leaseSeconds,
            @Param("limit") int limit
    );

    @Modifying
    @Query(value = """
        UPDATE outbox_delivery
        SET status = 'DELIVERED',
            delivered_at = now(),
            claimed_by = null,
            claimed_until = null,
            last_error = null,
            next_retry_at = null,
            update_time = now(),
            update_user = :actor
        WHERE target_id = :targetId
          AND claimed_by = :consumerId
          AND status = 'CLAIMED'
          AND id IN (:ids)
        """, nativeQuery = true)
    int ack(
            @Param("targetId") long targetId,
            @Param("consumerId") String consumerId,
            @Param("ids") Collection<Long> ids,
            @Param("actor") String actor
    );

    @Modifying
    @Query(value = """
        UPDATE outbox_delivery
        SET status = 'NEW',
            claimed_by = null,
            claimed_until = null,
            last_error = :error,
            next_retry_at = :nextRetryAt,
            update_time = now(),
            update_user = :actor
        WHERE target_id = :targetId
          AND claimed_by = :consumerId
          AND status = 'CLAIMED'
          AND id IN (:ids)
        """, nativeQuery = true)
    int nackRetryLater(
            @Param("targetId") long targetId,
            @Param("consumerId") String consumerId,
            @Param("ids") Collection<Long> ids,
            @Param("error") String error,
            @Param("nextRetryAt") OffsetDateTime nextRetryAt,
            @Param("actor") String actor
    );

    @Modifying
    @Query(value = """
        UPDATE outbox_delivery
        SET status = 'DEAD',
            claimed_by = null,
            claimed_until = null,
            last_error = :error,
            update_time = now(),
            update_user = :actor
        WHERE target_id = :targetId
          AND claimed_by = :consumerId
          AND status = 'CLAIMED'
          AND id IN (:ids)
        """, nativeQuery = true)
    int nackDead(
            @Param("targetId") long targetId,
            @Param("consumerId") String consumerId,
            @Param("ids") Collection<Long> ids,
            @Param("error") String error,
            @Param("actor") String actor
    );
}
