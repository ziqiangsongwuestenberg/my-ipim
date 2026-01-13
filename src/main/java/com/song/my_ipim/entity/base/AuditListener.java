package com.song.my_ipim.entity.base;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;

public class AuditListener {

    @PrePersist
    public void prePersist(BaseEntity e) {
        var now = OffsetDateTime.now();
        e.creationTime = now;
        e.lastUpdate = now;

    }

    @PreUpdate
    public void preUpdate(BaseEntity e) {
        e.lastUpdate = OffsetDateTime.now();
    }
}

