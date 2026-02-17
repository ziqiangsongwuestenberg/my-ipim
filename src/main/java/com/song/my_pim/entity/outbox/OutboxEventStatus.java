package com.song.my_pim.entity.outbox;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    SENT,
    FAILED,
    DEAD
}
