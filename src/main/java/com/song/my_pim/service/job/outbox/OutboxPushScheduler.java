package com.song.my_pim.service.job.outbox;

import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.repository.delivery.DeliveryTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "myipim.outbox.push", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPushScheduler {

    private final OutboxPushWorker outboxPushWorker;
    private final DeliveryTargetRepository deliveryTargetRepository;

    @Scheduled(fixedDelayString = "60000")
    public void tick() {
        List<DeliveryTargetEntity> enabledTargetList = deliveryTargetRepository.findByEnabledTrue();

        for (DeliveryTargetEntity target : enabledTargetList) {
            try {
                outboxPushWorker.publishDueEvents(target.getId());
            } catch (Exception ex) {
                log.error("Outbox publish tick failed for targetId={}", target.getId(), ex);
            }
        }
    }
}

