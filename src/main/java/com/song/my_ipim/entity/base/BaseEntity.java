package com.song.my_ipim.entity.base;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "creation_time", nullable = false, updatable = false)
    protected OffsetDateTime creationTime;

    @Column(name = "update_time", nullable = false)
    protected OffsetDateTime updateTime;

    @Column(name = "creation_user", nullable = false, updatable = false, length = 100)
    protected String creationUser;

    @Column(name = "update_user", nullable = false, length = 100)
    protected String updateUser;
}
