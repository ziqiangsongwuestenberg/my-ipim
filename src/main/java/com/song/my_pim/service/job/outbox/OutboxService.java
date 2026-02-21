package com.song.my_pim.service.job.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobHistoryEntity;
import com.song.my_pim.entity.outbox.OutboxDeliveryEntity;
import com.song.my_pim.entity.outbox.OutboxEventEntity;
import com.song.my_pim.entity.outbox.OutboxDeliveryStatus;
import com.song.my_pim.entity.outbox.OutboxEventType;
import com.song.my_pim.repository.delivery.DeliveryTargetRepository;
import com.song.my_pim.repository.outbox.OutboxDeliveryRepository;
import com.song.my_pim.repository.outbox.OutboxEventRepository;
import com.song.my_pim.service.job.DispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadJsonFactory outboxPayloadJsonFactory;
    private final OutboxDeliveryService outboxDeliveryService;

    public void enqueueExportCompleted(JobEntity job, JobHistoryEntity jobHistory, DispatchResult dispatchResult) {
        UUID eventUid = UUID.randomUUID();

        OutboxEventEntity outboxEvent = new OutboxEventEntity();
        outboxEvent.setEventUid(eventUid);
        outboxEvent.setAggregateType(ExportConstants.JOB_HISTORY);
        outboxEvent.setAggregateId(jobHistory.getId());
        outboxEvent.setEventType(OutboxEventType.EXPORT_COMPLETED);
        JsonNode payload = outboxPayloadJsonFactory.exportCompleted(eventUid, job, jobHistory, dispatchResult);
        outboxEvent.setPayloadJson(payload);
        // audit
        outboxEvent.setCreationUser(ExportConstants.SYSTEM);
        outboxEvent.setUpdateUser(ExportConstants.SYSTEM);
        outboxEvent.setCreationTime(OffsetDateTime.now());
        outboxEvent.setUpdateTime(OffsetDateTime.now());

        outboxEventRepository.save(outboxEvent);


        outboxDeliveryService.addOutboxDelivery(job.getClientId(), outboxEvent);
    }


}

