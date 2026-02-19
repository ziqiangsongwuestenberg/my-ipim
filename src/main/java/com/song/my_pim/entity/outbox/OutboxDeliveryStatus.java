package com.song.my_pim.entity.outbox;

public enum OutboxDeliveryStatus {
    NEW,
    PROCESSING,
    SENT,
    FAILED,
    DEAD
}
