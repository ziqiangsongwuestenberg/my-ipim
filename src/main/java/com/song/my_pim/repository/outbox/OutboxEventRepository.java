package com.song.my_pim.repository.outbox;

import com.song.my_pim.entity.outbox.OutboxEventEntity;
import com.song.my_pim.entity.outbox.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    Optional<OutboxEventEntity> findByEventUid(UUID eventUid);

    @Query("""
           select e
           from OutboxEventEntity e
           where e.status in :statuses
             and (e.nextRetryAt is null or e.nextRetryAt <= :now)
           order by e.creationTime asc
           """)
    List<OutboxEventEntity> findDueEvents(@Param("statuses") List<OutboxEventStatus> statuses,
                                          @Param("now") OffsetDateTime now,
                                          Pageable pageable);
}
