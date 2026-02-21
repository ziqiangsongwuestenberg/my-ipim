package com.song.my_pim.dto.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.song.my_pim.entity.outbox.OutboxEventType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
public class OutboxDeliveryItemDto {
    //outbox_delivery table
    private Long deliveryId;
    private int attemptCount;
    private OffsetDateTime leasedUntil;
    //outbox_event table
    private UUID eventUid;
    private OutboxEventType eventType;
    private JsonNode payload;
}