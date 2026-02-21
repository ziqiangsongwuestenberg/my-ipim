package com.song.my_pim.service.job.outbox;

import com.song.my_pim.dto.outbox.*;
import com.song.my_pim.repository.outbox.OutboxDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPullService {
    private final OutboxDeliveryRepository outboxDeliveryRepository;

    //claim/lease and return the payload json
    @Transactional
    public OutboxPullClaimResponse claim(OutboxPullClaimRequest request){

        String consumerId = request.getConsumerId();
        Integer batchSize = request.getBatchSize() == null ? 3 : request.getBatchSize();
        Integer leaseSeconds = request.getLeaseSeconds() == null ? 120 : request.getLeaseSeconds();
        String targetKey = request.getTargetKey();

        log.info("Start to claim for pulling, consumerId: {}, targetKey : {}, batchSize : {}, leaseSeconds : {}",consumerId, targetKey, batchSize, leaseSeconds);

        // claim/ lease + SKIP LOCKED
        List<Long> deliveryIds = outboxDeliveryRepository.claimBatchForPull(targetKey, consumerId, leaseSeconds, batchSize);

        if (deliveryIds.isEmpty()) {
            OutboxPullClaimResponse resp = new OutboxPullClaimResponse();
            resp.setItems(Collections.emptyList());
            log.info("No delivery item found for pulling for consumerId: {}, targetKey : {}", consumerId, targetKey);
            return resp;
        }

        // fetch : by targetId and status
        List<OutboxDeliveryItemDto> itemList = outboxDeliveryRepository.findDeliveryItemByIdsForPull(deliveryIds);
        OutboxPullClaimResponse response = new OutboxPullClaimResponse();
        response.setItems(itemList);
        log.info("{} deliveries claimed for pulling for consumerId: {}, targetKey : {}", itemList.size(), consumerId, targetKey);
        return response;
    }

    //Mark the deliveries as completed after the consumer processes them successfully, and release the lease.
    @Transactional
    public OutboxPullAckResponse ack(OutboxPullAckRequest request){
        String consumerId = request.getConsumerId();
        List<Long> deliveryIds = request.getDeliveryIds();
        log.info("Start to ack for pulling for consumerId: {}, deliveryIds : {}", consumerId, deliveryIds);
        int ackedCount = outboxDeliveryRepository.ackForPull(consumerId, deliveryIds);

        OutboxPullAckResponse response = new OutboxPullAckResponse();
        response.setAckedCount(ackedCount);
        log.info("{} deliveries acked for pulling for consumerId: {}", ackedCount, consumerId);
        return response;
    }

    /**
     * Currently, in this nack method, I set "status = 'DEAD'", later after I finish retry function, this part should change to "status = 'FAILED'"
     */
    @Transactional
    public OutboxPullNackResponse nack(OutboxPullNackRequest request){
        String consumerId = request.getConsumerId();
        List<Long> deliveryIds = request.getDeliveryIds();
        String reason = request.getReason();
        log.info("Start to nack for pulling for consumerId: {}, deliveryIds : {}", consumerId, deliveryIds);
        int nackedCount = outboxDeliveryRepository.nackForPull(consumerId, deliveryIds, reason);

        OutboxPullNackResponse response = new OutboxPullNackResponse();
        if(nackedCount == 0){
            log.info("{} deliveries nacked for pulling for consumerId: {}", nackedCount, consumerId);
            response.setNackedCount(0);
            response.setMessage("Nack failed. Lease expired or not owned by this consumer");
            return response;
        }

        response.setNackedCount(nackedCount);
        response.setMessage(nackedCount + " deliveries nacked for pulling for consumerId: " + consumerId);
        log.info("{} deliveries nacked for pulling for consumerId: {}", nackedCount, consumerId);
        return response;
    }
}
