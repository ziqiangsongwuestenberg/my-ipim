package com.song.my_pim.dto.outbox;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OutboxPullClaimResponse {
    private List<OutboxDeliveryItemDto> items;
}
