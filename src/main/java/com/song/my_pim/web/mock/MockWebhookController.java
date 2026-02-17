package com.song.my_pim.web.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mock")
public class MockWebhookController {

    @PostMapping("/webhook")
    public ResponseEntity<String> receive(
            @RequestBody(required = false) String body,
            @RequestHeader Map<String, String> headers
    ) {
        log.info("Mock webhook received. headers={}", headers);
        log.info("Mock webhook body={}", body);
        return ResponseEntity.ok("ok");
    }
}
