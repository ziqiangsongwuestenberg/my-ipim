package com.song.my_pim.entity.article;

import com.song.my_pim.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "article")
public class Article extends BaseEntity {

    @Column(name = "article_type", nullable = false, length = 20)
    private String articleType; // 'Article' or 'Product'

    @Column(name = "article_no", nullable = false, length = 80, unique = true)
    private String articleNo;

    @Column(name = "product_no", length = 80)
    private String productNo;

    @Column(name = "client", nullable = false)
    private Integer client;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "status1")
    private Integer status1;

    @Column(name = "status2")
    private Integer status2;

    @Column(name = "status3")
    private Integer status3;

    @Column(name = "status4")
    private Integer status4;

}