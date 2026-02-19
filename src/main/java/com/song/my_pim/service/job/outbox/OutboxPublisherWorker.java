package com.song.my_pim.service.job.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.entity.delivery.DeliveryTargetType;
import com.song.my_pim.entity.outbox.OutboxDeliveryEntity;
import com.song.my_pim.entity.outbox.OutboxDeliveryStatus;
import com.song.my_pim.repository.outbox.OutboxDeliveryRepository;
import com.song.my_pim.service.job.delivery.DeliveryAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OutboxPublisherWorker {
    private static final int BATCH_SIZE = 20;
    private static final int MAX_ATTEMPTS = 10;
    private static final int LEASE_SECONDS = 60;

    private final Map<DeliveryTargetType, DeliveryAdapter> targetTypeToDeliveryAdapterMap = new EnumMap<>(DeliveryTargetType.class);
    private final OutboxDeliveryRepository outboxDeliveryRepository;

    public OutboxPublisherWorker(
            ObjectMapper objectMapper,
            List<DeliveryAdapter> adapterList,
            OutboxDeliveryRepository outboxDeliveryRepository) {

        for (DeliveryAdapter deliveryAdapter : adapterList) {
            targetTypeToDeliveryAdapterMap.put(deliveryAdapter.type(), deliveryAdapter);
        }
        this.outboxDeliveryRepository = outboxDeliveryRepository;
    }

    // claim -> fetch -> process
    @Transactional
    public void publishDueEvents(long targetId) {
        //claim
        List<Long> deliveryIds = outboxDeliveryRepository.claimDueDeliveryIds(
                targetId,
                ExportConstants.SYSTEM,
                LEASE_SECONDS,
                BATCH_SIZE
        );
        if (deliveryIds.isEmpty()) return;

        // fetch : status IN ('NEW','FAILED') / ('PROCESSING' && claimed_until < now()) -> last time failed
        List<OutboxDeliveryEntity> outboxDeliveries = outboxDeliveryRepository.findAllByIds(deliveryIds);

        for (OutboxDeliveryEntity d : outboxDeliveries) {
            processOne(d);
        }
    }

    private void processOne(OutboxDeliveryEntity outboxDelivery) {
        MDC.put("deliveryId", String.valueOf(outboxDelivery.getId()));
        MDC.put("eventId", String.valueOf(outboxDelivery.getOutboxEvent().getEventUid()));
        MDC.put("eventType", String.valueOf(outboxDelivery.getOutboxEvent().getEventType()));
        MDC.put("attempt", String.valueOf(outboxDelivery.getAttemptCount())); // in claim did +1

        try {

            DeliveryTargetEntity target = outboxDelivery.getTarget();
                    ;
            MDC.put(ExportConstants.CLIENT_ID, String.valueOf(target.getClientId()));
            MDC.put("targetId", String.valueOf(target.getId()));
            MDC.put("targetType", String.valueOf(target.getType()));

            DeliveryAdapter deliveryAdapter = targetTypeToDeliveryAdapterMap.get(target.getType());
            if (deliveryAdapter == null) {
                throw new IllegalStateException("No deliveryAdapter for type=" + target.getType());
            }

            deliveryAdapter.deliver(outboxDelivery); // deliver to target !!!


            markOutboxDeliveryAsSent(outboxDelivery);

        } catch (Exception ex) {
            markOutboxDeliveryOnFailure(outboxDelivery, ex);
        } finally {
            MDC.clear();
        }
    }

    private void markOutboxDeliveryAsSent(OutboxDeliveryEntity outboxDelivery) {
        outboxDelivery.setStatus(OutboxDeliveryStatus.SENT);
        outboxDelivery.setDeliveredAt(OffsetDateTime.now());
        outboxDelivery.setLastError(null);
        outboxDelivery.setNextRetryAt(null);
        outboxDelivery.setClaimedBy(null);
        outboxDelivery.setClaimedUntil(null);
        outboxDelivery.setUpdateUser(ExportConstants.SYSTEM);
        outboxDelivery.setUpdateTime(OffsetDateTime.now());
        outboxDeliveryRepository.save(outboxDelivery);
    }

    private void markOutboxDeliveryOnFailure(OutboxDeliveryEntity outboxDelivery, Exception exception) {
        int attempts = outboxDelivery.getAttemptCount();
        outboxDelivery.setAttemptCount(attempts);

        String msg = exception.getMessage();
        if (msg != null && msg.length() > 2000) msg = msg.substring(0, 2000);
        outboxDelivery.setLastError(msg);

        if (attempts >= MAX_ATTEMPTS) {
            outboxDelivery.setStatus(OutboxDeliveryStatus.DEAD);
            outboxDelivery.setNextRetryAt(null);
            log.error("Outbox event DEAD. eventId={}, attempts={}, err={}", outboxDelivery.getOutboxEvent().getEventUid(), attempts, msg, exception);
        } else {
            outboxDelivery.setStatus(OutboxDeliveryStatus.FAILED);
            outboxDelivery.setNextRetryAt(OffsetDateTime.now().plusSeconds(backoffSeconds(attempts)));
            log.warn("Outbox delivery failed. eventId={}, attempts={}, nextRetryAt={}, err={}",
                    outboxDelivery.getOutboxEvent().getEventUid(), attempts, outboxDelivery.getNextRetryAt(), msg);
        }

        outboxDelivery.setUpdateUser(ExportConstants.SYSTEM);
        outboxDelivery.setUpdateTime(OffsetDateTime.now());
        outboxDeliveryRepository.save(outboxDelivery);
    }

    private long backoffSeconds(int attempt) {
        // 2,4,8,16,32... capped to 300s
        long s = 1L << Math.min(attempt, 8); // 2^attempt capped
        return Math.min(300, s);
    }

}
