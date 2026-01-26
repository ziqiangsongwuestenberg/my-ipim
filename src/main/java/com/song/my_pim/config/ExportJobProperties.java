package com.song.my_pim.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

@Data
@Component
@ConfigurationProperties(prefix = "mypim.export")
public class ExportJobProperties {
    private Integer client; // default value
    private List<String> attributeWhitelist = new ArrayList<>();
    private List<String> priceWhitelist = new ArrayList<>();
    private Statuses statuses = new Statuses();
    private int pageSize = 500;
    private boolean includeDeleted = false;
    private String fileName;
    private int threadCount = 4;
    private Payload payload = new Payload();

    @Data
    public static class Payload {
        private Path baseDir;
        private String filePrefix = "articles-export";
    }

    @Data
    public static class Statuses {
        private List<Integer> status1 = new ArrayList<>();
        private List<Integer> status2 = new ArrayList<>();
        private List<Integer> status3 = new ArrayList<>();
        private List<Integer> status4 = new ArrayList<>();
    }
}
