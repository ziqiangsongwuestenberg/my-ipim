package com.song.my_pim.dto.outbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboxPullClaimRequest {
    @NotBlank
    private String consumerId;
    private Integer batchSize;
    private Integer leaseSeconds;
    @Size(max=100)
    private String targetKey; //(nullable)
    private String eventType; //(nullable)
}
