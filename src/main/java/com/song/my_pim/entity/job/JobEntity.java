package com.song.my_pim.entity.job;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job")
public class JobEntity extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 80)
    private JobType jobType;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    @Column(nullable = false, length = 100)
    private String cron;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json", nullable = false, columnDefinition = "jsonb")
    private String paramsJson;

    @Column(name = "next_run_at", nullable = false)
    private OffsetDateTime nextRunAt;


    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (creationTime == null) creationTime = now;
        if (updateTime == null) updateTime = now;
        if (creationUser == null) creationUser = ExportConstants.SYSTEM;
        if (updateUser == null) updateUser = ExportConstants.SYSTEM;
        if (paramsJson == null) paramsJson = "{}";
    }

    @PreUpdate
    void preUpdate() {
        updateTime  = OffsetDateTime.now();
        if (updateUser == null) updateUser = ExportConstants.SYSTEM;    }
}
