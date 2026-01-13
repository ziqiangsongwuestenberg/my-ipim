package com.song.my_ipim.entity.article;

import com.song.my_ipim.entity.base.BaseEntity;
import com.song.my_ipim.entity.attribute.Attribute;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter @Setter
@Entity
@Table(name = "article_av",
        uniqueConstraints = @UniqueConstraint(name="ux_article_av",
                columnNames = {"article_id","attribute_id","value_index"}))
public class ArticleAV extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="article_id", nullable=false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="attribute_id", nullable=false)
    private Attribute attribute;

    @Column(name = "client", nullable = false)
    private Integer client;

    @Column(name="value_text")
    private String valueText;

    @Column(name="value_num", precision = 18, scale = 6)
    private BigDecimal valueNum;

    @Column(name="value_bool")
    private Boolean valueBool;

    @Column(name="value_date")
    private LocalDate valueDate;

    @Column(name="value_index", nullable=false)
    private int valueIndex;

    @Column(nullable=false)
    private boolean deleted;

    @Column(name="update_time", nullable=false)
    private OffsetDateTime updateTime;
}