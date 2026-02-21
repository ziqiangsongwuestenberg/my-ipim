package com.song.my_pim.service.job.outbox;

import com.song.my_pim.repository.outbox.OutboxDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OutboxLeaseReaperService {

    private final OutboxDeliveryRepository outboxDeliveryRepository;

    public int releaseExpiredLeases() {
        log.info("Start to release expired outbox delivery leases");
        int count = outboxDeliveryRepository.releaseExpiredLeases("lease-reaper");
        if(count == 0){
            log.info("No expired outbox delivery leases found");
        }
        return count;
    }
}
