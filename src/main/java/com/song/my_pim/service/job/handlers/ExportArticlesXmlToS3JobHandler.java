package com.song.my_pim.service.job.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.song.my_pim.dto.exportjob.ArticleExportRequest;
import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobType;
import com.song.my_pim.service.exportjob.ArticleAsyncExportJobService;
import com.song.my_pim.service.job.DispatchResult;
import com.song.my_pim.service.job.JobHandler;
import com.song.my_pim.service.job.model.ArtifactInfo;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ExportArticlesXmlToS3JobHandler implements JobHandler {

    private final ArticleAsyncExportJobService articleAsyncExportJobService;
    private final ObjectMapper objectMapper;

    public ExportArticlesXmlToS3JobHandler(ArticleAsyncExportJobService articleAsyncExportJobService,
                                           ObjectMapper objectMapper) {
        this.articleAsyncExportJobService = articleAsyncExportJobService;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobType supports() {
        return JobType.EXPORT_ARTICLES_XML_TO_S3;
    }

    @Override
    public DispatchResult execute(JobEntity job) throws Exception {
        MDC.put("jobType", supports().name());
        MDC.put("clientId", String.valueOf(job.getClientId()));
        MDC.put("jobId", String.valueOf(job.getId()));

        try {
            log.info("Export job started!!!");

            // params_json -> ArticleExportRequest
            ArticleExportRequest request = objectMapper.readValue(job.getParamsJson(), ArticleExportRequest.class);

            Integer client = job.getClientId();
            String s3Uri = articleAsyncExportJobService.exportArticlesXmlToS3(client, request);

            log.info("job.completed");

            // store result_json (as JSON string)
            String resultJson = objectMapper.writeValueAsString(Map.of("s3Uri", s3Uri));
            ArtifactInfo artifact = new ArtifactInfo(s3Uri, null, null, "XML", "v1");

            return new DispatchResult(resultJson, artifact);
        } catch (Exception ex) {
            log.error("job.failed", ex);
            throw ex;
        } finally {
            // Always clear MDC to prevent leaking context to the next job executed by the same thread.
            MDC.clear();
        }

    }
}
