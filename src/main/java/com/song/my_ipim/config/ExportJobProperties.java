package com.song.my_ipim.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Data
@Component
@ConfigurationProperties(prefix = "myipim.export")
public class ExportJobProperties {
    private Integer client;
    private List<String> attributeWhitelist = new ArrayList<>();
    private Statuses statuses = new Statuses();
    private int pageSize = 500;
    private boolean includeDeleted = false;

    @Data
    public static class Statuses {
        private List<Integer> status1 = new ArrayList<>();
        private List<Integer> status2 = new ArrayList<>();
        private List<Integer> status3 = new ArrayList<>();
        private List<Integer> status4 = new ArrayList<>();
    }
}
