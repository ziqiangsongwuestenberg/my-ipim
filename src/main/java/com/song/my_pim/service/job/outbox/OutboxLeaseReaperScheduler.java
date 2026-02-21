package com.song.my_pim.service.job.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "myipim.outbox.lease.repare", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxLeaseReaperScheduler {
    private final OutboxLeaseReaperService outboxLeaseReaperService;

    @Scheduled(fixedRateString = "60000") // here this number should be smaller, maybe 10s to 30s, but here for testing purpose, I set it to 1 minute
    public void tick(){
        int count = outboxLeaseReaperService.releaseExpiredLeases();
        if(count > 0){
            log.info("Released {} expired outbox delivery leases", count);
        }
    }
}
