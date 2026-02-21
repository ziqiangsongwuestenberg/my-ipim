package com.song.my_pim.repository.outbox;

import com.song.my_pim.dto.outbox.OutboxDeliveryItemDto;
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
              OR (d.status = 'PROCESSING' AND d.claimed_until IS NULL) -- released by scheduled job
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
            JOIN delivery_target t on d.target_id = t.id
            WHERE (:targetKey IS NULL or t.target_key = :targetKey)
              AND (
                    d.status IN ('NEW','FAILED')
                    OR (d.status = 'PROCESSING' AND d.claimed_until IS NOT NULL AND d.claimed_until < now()) -- last time failed
                    OR (d.status = 'PROCESSING' AND d.claimed_until IS NULL) -- released by scheduled job
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
          --attempt_count = d.attempt_count ,
            update_time = now(),
            update_user = :user
        FROM cte
        WHERE d.id = cte.id
        RETURNING d.id
        """, nativeQuery = true)
    List<Long> claimBatchForPull(
            @Param("targetKey") String targetKey,
            @Param("user") String user,
            @Param("leaseSeconds") int leaseSeconds,
            @Param("limit") int limit
    );

    @Modifying
    @Query(value = """
        UPDATE outbox_delivery
        SET status = 'SENT',
            delivered_at = now(),
            claimed_by = null,
            claimed_until = null,
            last_error = null,
            next_retry_at = null,
            update_time = now(),
            update_user = :user
        WHERE 
              claimed_by = :user
          AND claimed_until > now()
          AND status = 'PROCESSING'
          AND id IN (:ids)
        """, nativeQuery = true)
    int ackForPull(
            @Param("user") String user,
            @Param("ids") Collection<Long> ids
    );

    @Query(value = """
        SELECT NEW com.song.my_pim.dto.outbox.OutboxDeliveryItemDto(
            d.id,
            d.attemptCount,
            d.claimedUntil,
            e.eventUid,
            e.eventType,
            e.payloadJson)     
        FROM OutboxDeliveryEntity d
        JOIN d.outboxEvent e
        WHERE d.id IN :deliveryIds
        ORDER BY d.id
            """)
    List<OutboxDeliveryItemDto> findDeliveryItemByIdsForPull(@Param("deliveryIds") Collection<Long> deliveryIds);

    @Modifying
    @Query(value = """
        UPDATE outbox_delivery
        SET status = 'DEAD',
            claimed_by = null,
            claimed_until = null,
            last_error = :lastError,
            next_retry_at = null,
            update_time = now(),
            update_user = :user
        WHERE 
              claimed_by = :user
          AND claimed_until > now()
          AND status = 'PROCESSING'
          AND id IN (:ids)
        """, nativeQuery = true)
    int nackForPull(
            @Param("user") String user,
            @Param("ids") Collection<Long> ids,
            @Param("lastError") String lastError
    );


    /**
     * I keep status = 'PROCESSING' here, because in PUSH and PULL process they still taking the outbox delivery items with (status = 'PROCESSING' AND claimed_until IS NOT NULL AND claimed_until < now())
     */
    @Modifying
    @Query(value = """
        UPDATE outbox_delivery
        SET 
            claimed_by = NULL,
            claimed_until = NULL,
            update_time = now(),
            update_user = :user
        WHERE status = 'PROCESSING'
        AND claimed_until IS NOT NULL
        AND claimed_until < now()
        """, nativeQuery = true)
    int releaseExpiredLeases(@Param("user") String user);
}
