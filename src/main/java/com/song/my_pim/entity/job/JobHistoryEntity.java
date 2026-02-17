package com.song.my_pim.entity.job;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_history")
public class JobHistoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Column(name = "scheduled_time", nullable = false)
    private OffsetDateTime scheduledTime;

    @Column(name = "run_uid", nullable = false, updatable = false)
    private UUID runUid;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "artifact_uri", columnDefinition = "text")
    private String artifactUri;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "output_format", length = 20)
    private String outputFormat;

    @Column(name = "schema_version", length = 50)
    private String schemaVersion;

    /**
     * JSON result of job execution.
     * Example:
     * {
     *   "s3Uri": "s3://bucket/client-1/articles-2026-02-06.xml"
     * }
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", nullable = false, columnDefinition = "jsonb")
    private String resultJson = "{}";

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (creationTime == null) creationTime = now;
        if (updateTime == null) updateTime = now;
        if (creationUser == null) creationUser = ExportConstants.SYSTEM;
        if (updateUser == null) updateUser = ExportConstants.SYSTEM;
        if (runUid == null) runUid = UUID.randomUUID();
        if (resultJson == null) resultJson = "{}";
    }
}

