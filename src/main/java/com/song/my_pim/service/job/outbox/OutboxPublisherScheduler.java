package com.song.my_pim.service.job.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxPublisherWorker outboxPublisherWorker;

    @Scheduled(fixedDelayString = "10000")
    public void tick() {
        outboxPublisherWorker.publishDueEvents();
    }
}
