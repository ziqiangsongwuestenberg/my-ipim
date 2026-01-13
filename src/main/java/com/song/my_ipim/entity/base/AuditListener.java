package com.song.my_ipim.entity.base;

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
            e.setCreationUser("system");
        }
        if (e.getUpdateUser() == null) {
            e.setUpdateUser("system");
        }

    }

    @PreUpdate
    public void preUpdate(BaseEntity e) {
        e.updateTime = OffsetDateTime.now();

        if (e.getUpdateUser() == null) {
            e.setUpdateUser("system");
        }
    }
}

