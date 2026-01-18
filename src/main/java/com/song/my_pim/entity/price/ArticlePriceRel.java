package com.song.my_pim.entity.price;

import com.song.my_pim.entity.article.Article;
import com.song.my_pim.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(
        name = "article_price_rel",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_article_price_rel",
                        columnNames = {"client", "article_id", "price_id", "valid_from"}
                )
        }
)
public class ArticlePriceRel extends BaseEntity {

    @Column(nullable = false)
    private Integer client;

    // ---- relations ----

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_id", nullable = false)
    private Price price;

    // ---- business fields ----

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal amount;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(nullable = false)
    private boolean deleted = false;

}
