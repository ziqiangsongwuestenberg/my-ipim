package com.song.my_pim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "my-pim.fault.s3")
public record S3FaultProperties(
        boolean enabled,
        int failFirstN
) {}
