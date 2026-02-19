package com.song.my_pim.entity.outbox;

import com.song.my_pim.entity.base.BaseEntity;
import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "outbox_delivery")
public class OutboxDeliveryEntity  extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outbox_event_id", nullable = false)
    private OutboxEventEntity outboxEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private DeliveryTargetEntity target;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private OutboxDeliveryStatus status = OutboxDeliveryStatus.NEW;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    //claim/lease
    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "claimed_until")
    private OffsetDateTime claimedUntil;

    @PrePersist
    void prePersist() {
        if (status == null) status = OutboxDeliveryStatus.NEW;
        if (attemptCount == null) attemptCount = 0;
    }
}
