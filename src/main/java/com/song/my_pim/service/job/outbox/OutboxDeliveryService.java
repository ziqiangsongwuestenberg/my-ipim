package com.song.my_pim.service.job.outbox;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.entity.outbox.OutboxDeliveryEntity;
import com.song.my_pim.entity.outbox.OutboxDeliveryStatus;
import com.song.my_pim.entity.outbox.OutboxEventEntity;
import com.song.my_pim.repository.delivery.DeliveryTargetRepository;
import com.song.my_pim.repository.outbox.OutboxDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxDeliveryService {
    private final DeliveryTargetRepository deliveryTargetRepository;
    private final OutboxDeliveryRepository outboxDeliveryRepository;

    // deliver to which target, is decided by the clientID
    public void addOutboxDelivery(Integer clientId, OutboxEventEntity outboxEvent) {
        List<DeliveryTargetEntity> deliveryTargetList = deliveryTargetRepository.findByClientIdAndEnabledTrue(clientId);

        List<OutboxDeliveryEntity> outboxDeliveryList = new ArrayList<>();
        for(DeliveryTargetEntity deliveryTarget : deliveryTargetList){
            outboxDeliveryList.add(newOutboxDelivery(outboxEvent, deliveryTarget));
        }

        outboxDeliveryRepository.saveAll(outboxDeliveryList);
    }

    public OutboxDeliveryEntity newOutboxDelivery(OutboxEventEntity outboxEvent, DeliveryTargetEntity deliveryTarget ){
        OutboxDeliveryEntity outboxDelivery = new OutboxDeliveryEntity();
        outboxDelivery.setOutboxEvent(outboxEvent);
        outboxDelivery.setTarget(deliveryTarget);
        outboxDelivery.setStatus(OutboxDeliveryStatus.NEW);
        outboxDelivery.setAttemptCount(0);
        outboxDelivery.setNextRetryAt(null);
        outboxDelivery.setLastError(null);
        outboxDelivery.setClaimedBy(null);
        outboxDelivery.setClaimedUntil(null);
        outboxDelivery.setCreationUser(ExportConstants.SYSTEM);
        outboxDelivery.setCreationTime(OffsetDateTime.now());
        outboxDelivery.setUpdateUser(ExportConstants.SYSTEM);
        outboxDelivery.setUpdateTime(OffsetDateTime.now());
        return outboxDelivery;
    }
}
