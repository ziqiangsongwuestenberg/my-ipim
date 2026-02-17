package com.song.my_pim.service.job.model;

public record ArtifactInfo(
        String uri,              // s3://bucket/key or s3Uri
        Long sizeBytes,
        String checksumSha256,
        String format,           // "XML"/"JSON"/"CSV"
        String schemaVersion     // "v1" .. or null
) {}