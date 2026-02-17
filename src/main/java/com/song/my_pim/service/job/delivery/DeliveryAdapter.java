package com.song.my_pim.service.job.delivery;

import com.song.my_pim.entity.delivery.DeliveryTargetEntity;
import com.song.my_pim.entity.delivery.DeliveryTargetType;
import com.song.my_pim.entity.outbox.OutboxEventEntity;

public interface DeliveryAdapter {
    DeliveryTargetType type(); // "WEBHOOK"
    void deliver(DeliveryTargetEntity target, OutboxEventEntity event) throws Exception;
}
