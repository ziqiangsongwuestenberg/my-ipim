package com.song.my_pim.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mypim.export.thread-pool")
public class ExportJobThreadPoolProperties {
    private int corePoolSize = 4;
    private int maxPoolSize = 8;
    private int queueCapacity = 200;
}
