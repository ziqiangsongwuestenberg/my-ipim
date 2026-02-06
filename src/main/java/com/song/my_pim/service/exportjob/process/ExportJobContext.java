package com.song.my_pim.service.exportjob.process;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.song.my_pim.service.exportjob.payload.ExportJobPayloadHandler;
import lombok.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobContext {

    private String jobId;
    private Integer client;

    private int chunkParts;

    // payload
    private String filePrefix;

    private Instant startTime;

    //Each job dynamically generated
    private Path payloadDir;

    // final merged file for export
    private Path exportFile;

    // job-specific parameters(e.g. ArticleExportRequest)
    @Builder.Default
    @JsonIgnore
    private Map<String, Object> attributes = new HashMap<>();

    @JsonIgnore
    private ExportJobPayloadHandler payloadHandler;

    public <T> T getAttr(String key, Class<T> type) {
        Object v = attributes.get(key);
        if (v == null) return null;
        return type.cast(v);
    }

    public void putAttr(String key, Object value) {
        attributes.put(key, value);
    }
}
