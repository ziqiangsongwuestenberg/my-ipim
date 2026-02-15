package com.song.my_pim.service.exportjob.s3Service;

import com.song.my_pim.common.exception.ExportWriteException;
import com.song.my_pim.config.S3Properties;
import com.song.my_pim.monitoring.S3FaultInjector;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportToS3Service {

    private final S3Client s3;
    private final S3Properties props;
    private final S3FaultInjector s3FaultInjector;
    /**
     * Uploads an XML file to S3.
     *
     * Resilience:
     * - Retry handles transient network/S3 glitches.
     * - CircuitBreaker prevents hammering S3 when it is unhealthy and protects the rest of the system.
     * - Bulkhead limits concurrent uploads to avoid exhausting threads/connections.
     */
    @Retry(name = "s3Upload", fallbackMethod = "uploadXmlFileFallback")
    @CircuitBreaker(name = "s3Upload", fallbackMethod = "uploadXmlFileFallback")
    @Bulkhead(name = "s3Upload", type = Bulkhead.Type.SEMAPHORE)
    public String uploadXmlFile(Path xmlFile, String objectKey) {
        // Build the final key with an optional prefix (avoid double slashes and blank prefixes).
        String key = (props.getPrefix() == null || props.getPrefix().isBlank()) ? objectKey : (props.getPrefix() + "/" + objectKey);

        MDC.put("s3Bucket", props.getBucket());
        MDC.put("s3Key", key);

        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(props.getBucket())
                    .key(key)
                    .contentType("application/xml")
                    .build();

            log.info("Uploading to bucket={}, key={}, region={}",
                    props.getBucket(), key, props.getRegion());

            s3FaultInjector.maybeFail("articles_xml");

            s3.putObject(req, RequestBody.fromFile(xmlFile));

            return "s3://" + props.getBucket() + "/" + key;
        } finally {
            MDC.remove("s3Bucket");
            MDC.remove("s3Key");
        }
    }

    /**
     * Fallback for Retry/CircuitBreaker.
     *
     * Signature rule: same args as the original method + Throwable as the last parameter.
     * We do NOT return a "fake success" value here; we fail fast so the caller can mark the job/chunk as failed
     * (and later support DLQ/replay).
     */
    private String uploadXmlFileFallback(Path xmlFile, String objectKey, Throwable ex) {
        // If you already put business identifiers into MDC (jobId/clientId/chunkId), they will show here.
        log.error("s3.upload.failed bucket={}, objectKey={}, jobId={}, clientId={}",
                props.getBucket(), objectKey, MDC.get("jobId"), MDC.get("clientId"), ex);

        throw new ExportWriteException("S3 upload failed (after retries / circuit breaker)", ex);
    }
}

