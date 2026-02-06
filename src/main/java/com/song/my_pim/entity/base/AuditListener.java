package com.song.my_pim.entity.base;

import com.song.my_pim.common.constants.ExportConstants;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;

public class AuditListener {

    @PrePersist
    public void prePersist(BaseEntity e) {
        var now = OffsetDateTime.now();
        e.creationTime = now;
        e.updateTime = now;

        if (e.getCreationUser() == null) {
            e.setCreationUser(ExportConstants.SYSTEM);
        }
        if (e.getUpdateUser() == null) {
            e.setUpdateUser(ExportConstants.SYSTEM);
        }

    }

    @PreUpdate
    public void preUpdate(BaseEntity e) {
        e.updateTime = OffsetDateTime.now();

        if (e.getUpdateUser() == null) {
            e.setUpdateUser(ExportConstants.SYSTEM);
        }
    }
}

