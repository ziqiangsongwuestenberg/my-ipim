package com.song.my_pim.service.exportjob.s3Service;

import com.song.my_pim.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportToS3Service {

    private final S3Client s3;
    private final S3Properties props;

    public String uploadXmlFile(Path xmlFile, String objectKey) {
        String key = (props.getPrefix() == null || props.getPrefix().isBlank()) ? objectKey : (props.getPrefix() + "/" + objectKey);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType("application/xml")
                .build();

        log.info("Uploading to bucket={}, key={}, region={}",
                props.getBucket(), key, props.getRegion());
        s3.putObject(req, RequestBody.fromFile(xmlFile));

        return "s3://" + props.getBucket() + "/" + key;
    }
}

