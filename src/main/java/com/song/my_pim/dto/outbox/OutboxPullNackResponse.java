package com.song.my_pim.dto.outbox;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboxPullNackResponse {
    private int nackedCount;
    private String message;
}
