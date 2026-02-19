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
    @NonNull
    private String jobId;

    @NonNull
    private Integer client;

    @NonNull
    private int chunkParts;

    // payload
    @NonNull
    private String filePrefix;

    @Builder.Default
    private Instant startTime = Instant.now();

    //Each job dynamically generated
    @NonNull
    private Path payloadDir;

    // final merged file for export
    private Path exportFile;

    // job-specific parameters(e.g. ArticleExportRequest)
    @Builder.Default
    @JsonIgnore
    private Map<String, Object> attributes = new HashMap<>();

    @JsonIgnore
    @NonNull
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
