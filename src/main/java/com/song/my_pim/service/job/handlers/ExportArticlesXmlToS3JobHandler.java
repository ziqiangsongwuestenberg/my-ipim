package com.song.my_pim.service.job.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.song.my_pim.dto.exportjob.ArticleExportRequest;
import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobType;
import com.song.my_pim.service.exportjob.ArticleAsyncExportJobService;
import com.song.my_pim.service.job.JobHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

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
    public String execute(JobEntity job) throws Exception {
        // params_json -> ArticleExportRequest
        ArticleExportRequest request = objectMapper.readValue(job.getParamsJson(), ArticleExportRequest.class);

        Integer client = job.getClientId();
        String s3Uri = articleAsyncExportJobService.exportArticlesXmlToS3(client, request);

        // store result_json (as JSON string)
        return objectMapper.writeValueAsString(Map.of(
                "s3Uri", s3Uri
        ));
    }
}
