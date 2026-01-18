package com.song.my_pim.repository;

import com.song.my_pim.dto.exportjob.ArticlePriceExportRow;
import com.song.my_pim.entity.price.ArticlePriceRel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticlePriceRelRepository
        extends JpaRepository<ArticlePriceRel, Long> {

    List<ArticlePriceRel> findByClientAndArticleId(Integer client, Long articleId);

    List<ArticlePriceRel> findByClientAndArticleIdAndDeletedFalse(
            Integer client, Long articleId
    );

    @Query("""
    select new com.song.my_pim.dto.exportjob.ArticlePriceExportRow(
        a.id,
        a.articleNo,
        p.identifier,
        r.amount,
        r.validFrom
    )
    from ArticlePriceRel r
      join r.article a
      join r.price p
    where r.client = :client
      and r.deleted = false
      and a.deleted = false
""")
    List<ArticlePriceExportRow> findPriceExportRows(@Param("client") Integer client);
}
