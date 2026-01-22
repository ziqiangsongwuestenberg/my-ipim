package com.song.my_pim.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mypim.s3")
public class S3Properties {

    //S3 bucket name
    private String bucket;

    private String prefix;

    private String region;
}

