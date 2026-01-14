package com.song.my_pim.entity.attribute;


import com.song.my_pim.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "attribute")
public class Attribute extends BaseEntity {

    @Column(nullable=false, unique = true, length=120)
    private String identifier;

    @Column(name="name", nullable=false)
    private String name;

    @Column(name="value_type", nullable=false, length=30)
    private String valueType; // STRING/NUMBER/BOOLEAN/DATE/ENUM

    @Column(length = 30)
    private String unit;

    @Column(name="is_multivalue", nullable=false)
    private boolean multivalue;

    @Column(nullable=false)
    private boolean deleted;
}

