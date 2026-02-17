package com.song.my_pim.service.job.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobHistoryEntity;
import com.song.my_pim.service.job.DispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxPayloadJsonFactory {

    private final ObjectMapper objectMapper;

    public JsonNode exportCompleted(UUID eventUid, JobEntity job, JobHistoryEntity run, DispatchResult dispatchResult) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("eventId", eventUid.toString());
        root.put("eventType", "ExportCompleted");
        root.put("occurredAt", OffsetDateTime.now().toString());

        Map<String, Object> jobObj = new LinkedHashMap<>();
        jobObj.put("jobId", job.getId());
        jobObj.put("jobType", String.valueOf(job.getJobType()));
        jobObj.put("clientId", job.getClientId());
        jobObj.put("runId", run.getId());
        jobObj.put("runUid", run.getRunUid() != null ? run.getRunUid().toString() : null);
        jobObj.put("scheduledTime", run.getScheduledTime() != null ? run.getScheduledTime().toString() : null);
        jobObj.put("startedAt", run.getStartedAt() != null ? run.getStartedAt().toString() : null);
        jobObj.put("finishedAt", run.getFinishedAt() != null ? run.getFinishedAt().toString() : null);
        root.put("job", jobObj);

        if (dispatchResult.artifactInfo() != null) {
            Map<String, Object> art = new LinkedHashMap<>();
            art.put("uri", dispatchResult.artifactInfo().uri());
            art.put("sizeBytes", dispatchResult.artifactInfo().sizeBytes());
            art.put("checksumSha256", dispatchResult.artifactInfo().checksumSha256());
            art.put("format", dispatchResult.artifactInfo().format());
            art.put("schemaVersion", dispatchResult.artifactInfo().schemaVersion());
            root.put("artifact", art);
        }

        root.put("result", dispatchResult.resultJson());

        return objectMapper.valueToTree(root);
    }
}
