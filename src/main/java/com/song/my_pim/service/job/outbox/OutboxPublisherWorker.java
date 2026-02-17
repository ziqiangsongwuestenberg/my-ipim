package com.song.my_pim.service.job.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.entity.delivery.DeliveryTargetType;
import com.song.my_pim.entity.outbox.OutboxEventEntity;
import com.song.my_pim.entity.outbox.OutboxEventStatus;
import com.song.my_pim.repository.delivery.DeliveryTargetRepository;
import com.song.my_pim.repository.outbox.OutboxEventRepository;
import com.song.my_pim.service.job.delivery.DeliveryAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
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

    private final OutboxEventRepository outboxEventRepository;
    private final DeliveryTargetRepository deliveryTargetRepository;
    private final ObjectMapper objectMapper;
    private final Map<DeliveryTargetType, DeliveryAdapter> targetTypeToDeliveryAdapterMap = new EnumMap<>(DeliveryTargetType.class);

    public OutboxPublisherWorker(
            OutboxEventRepository outboxEventRepository,
            DeliveryTargetRepository targetRepo,
            ObjectMapper objectMapper,
            List<DeliveryAdapter> adapterList
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.deliveryTargetRepository = targetRepo;
        this.objectMapper = objectMapper;

        for (DeliveryAdapter deliveryAdapter : adapterList) {
            targetTypeToDeliveryAdapterMap.put(deliveryAdapter.type(), deliveryAdapter);
        }
    }

    @Transactional
    public void publishDueEvents() {
        OffsetDateTime now = OffsetDateTime.now();

        List<OutboxEventEntity> events = outboxEventRepository.findDueEvents(
                List.of(OutboxEventStatus.NEW, OutboxEventStatus.FAILED),
                now,
                PageRequest.of(0, BATCH_SIZE)
        );

        if (events.isEmpty()) return;

        for (OutboxEventEntity e : events) {
            processOne(e);
        }
    }

    private void processOne(OutboxEventEntity outboxEventEntity) {
        MDC.put("eventId", String.valueOf(outboxEventEntity.getEventUid()));
        MDC.put("eventType", String.valueOf(outboxEventEntity.getEventType()));
        MDC.put("attempt", String.valueOf(outboxEventEntity.getAttemptCount() + 1));

        outboxEventEntity.setStatus(OutboxEventStatus.PROCESSING);
        outboxEventEntity.setUpdateUser(ExportConstants.SYSTEM);
        outboxEventEntity.setUpdateTime(OffsetDateTime.now());
        outboxEventRepository.save(outboxEventEntity);

        try {
            Integer clientId = extractClientId(outboxEventEntity); // from payloadJson
            MDC.put(ExportConstants.CLIENT_ID, String.valueOf(clientId));

            List<DeliveryTargetEntity> deliveryTargetEntityList = deliveryTargetRepository.findByClientIdAndEnabledTrue(clientId);
            if (deliveryTargetEntityList.isEmpty()) {
                log.info("No delivery deliveryTargetEntityList. Mark SENT. eventId={}, clientId={}", outboxEventEntity.getEventUid(), clientId);
                markOutboxEventEntityAsSent(outboxEventEntity);
                return;
            }

            for (DeliveryTargetEntity deliveryTargetEntity : deliveryTargetEntityList) {
                DeliveryAdapter deliveryAdapter = targetTypeToDeliveryAdapterMap.get(deliveryTargetEntity.getType());
                if (deliveryAdapter == null) {
                    throw new IllegalStateException("No deliveryAdapter for type=" + deliveryTargetEntity.getType());
                }
                deliveryAdapter.deliver(deliveryTargetEntity, outboxEventEntity); // deliver to target !!!
            }

            markOutboxEventEntityAsSent(outboxEventEntity);

        } catch (Exception ex) {
            markOutboxEventEntityOnFailure(outboxEventEntity, ex);
        } finally {
            MDC.remove("eventId");
            MDC.remove("eventType");
            MDC.remove("attempt");
            MDC.remove(ExportConstants.CLIENT_ID);
        }
    }

    private Integer extractClientId(OutboxEventEntity e) throws Exception {
        // payload schema: { "job": { "clientId": ... } }
        JsonNode root = objectMapper.readTree(e.getPayloadJson());
        JsonNode clientNode = root.path("job").path(ExportConstants.CLIENT_ID);
        if (clientNode.isMissingNode() || clientNode.isNull()) {
            throw new IllegalStateException("payload.job.clientId missing. eventId=" + e.getEventUid());
        }
        return clientNode.asInt();
    }

    private void markOutboxEventEntityAsSent(OutboxEventEntity outboxEventEntity) {
        outboxEventEntity.setStatus(OutboxEventStatus.SENT);
        outboxEventEntity.setSentAt(OffsetDateTime.now());
        outboxEventEntity.setLastError(null);
        outboxEventEntity.setNextRetryAt(null);
        outboxEventEntity.setUpdateUser(ExportConstants.SYSTEM);
        outboxEventEntity.setUpdateTime(OffsetDateTime.now());
        outboxEventRepository.save(outboxEventEntity);
    }

    private void markOutboxEventEntityOnFailure(OutboxEventEntity outboxEventEntity, Exception exception) {
        int attempts = outboxEventEntity.getAttemptCount() + 1;
        outboxEventEntity.setAttemptCount(attempts);

        String msg = exception.getMessage();
        if (msg != null && msg.length() > 2000) msg = msg.substring(0, 2000);
        outboxEventEntity.setLastError(msg);

        if (attempts >= MAX_ATTEMPTS) {
            outboxEventEntity.setStatus(OutboxEventStatus.DEAD);
            outboxEventEntity.setNextRetryAt(null);
            log.error("Outbox event DEAD. eventId={}, attempts={}, err={}", outboxEventEntity.getEventUid(), attempts, msg, exception);
        } else {
            outboxEventEntity.setStatus(OutboxEventStatus.FAILED);
            outboxEventEntity.setNextRetryAt(OffsetDateTime.now().plusSeconds(backoffSeconds(attempts)));
            log.warn("Outbox delivery failed. eventId={}, attempts={}, nextRetryAt={}, err={}",
                    outboxEventEntity.getEventUid(), attempts, outboxEventEntity.getNextRetryAt(), msg);
        }

        outboxEventEntity.setUpdateUser(ExportConstants.SYSTEM);
        outboxEventEntity.setUpdateTime(OffsetDateTime.now());
        outboxEventRepository.save(outboxEventEntity);
    }

    private long backoffSeconds(int attempt) {
        // 2,4,8,16,32... capped to 300s
        long s = 1L << Math.min(attempt, 8); // 2^attempt capped
        return Math.min(300, s);
    }

}
