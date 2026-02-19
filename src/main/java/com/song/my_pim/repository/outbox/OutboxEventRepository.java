package com.song.my_pim.repository.outbox;

import com.song.my_pim.entity.outbox.OutboxEventEntity;
import com.song.my_pim.entity.outbox.OutboxDeliveryStatus;
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

}
