package com.song.my_pim.dto.outbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OutboxPullNackRequest {
    @NotBlank
    private String consumerId;
    @NotEmpty
    private List<Long> deliveryIds;

    private String reason;
}
