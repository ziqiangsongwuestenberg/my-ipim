package com.song.my_pim.web.outbox;

import com.song.my_pim.dto.outbox.*;
import com.song.my_pim.service.job.outbox.OutboxPullService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/outbox/pull", produces = MediaType.APPLICATION_JSON_VALUE)
public class OutboxPullController {

    private final OutboxPullService outboxPullService;

    // claim / lease
    @PostMapping(value = "/claim", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OutboxPullClaimResponse claim(@RequestBody OutboxPullClaimRequest request){
        OutboxPullClaimResponse response = outboxPullService.claim(request);
        return response;
    }

    @PostMapping(value = "/ack", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OutboxPullAckResponse ack(@RequestBody OutboxPullAckRequest request){
        OutboxPullAckResponse response = outboxPullService.ack(request);
        return response;
    }

    @PostMapping(value = "/nack", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OutboxPullNackResponse ack(@RequestBody OutboxPullNackRequest request){
        OutboxPullNackResponse response = outboxPullService.nack(request);
        return response;
    }

}
