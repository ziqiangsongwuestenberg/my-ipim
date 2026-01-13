package com.song.my_ipim.repository;

import com.song.my_ipim.dto.export.ArticleAvExportRow;
import com.song.my_ipim.entity.article.ArticleAV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ArticleAvRepository extends JpaRepository<ArticleAV, Long> {

    @Query("""
        select new com.song.my_ipim.dto.export.ArticleAvExportRow(
            a.id,
            a.articleNo,
            a.productNo,
            attr.identifier,
            attr.valueType,
            attr.unit,
            av.valueIndex,
            av.valueText,
            av.valueNum,
            av.valueBool,
            av.valueDate
        )
        from ArticleAV av
        join av.article a
        join av.attribute attr
        where a.id in :articleIds
          and a.client = :client
          and av.client = :client
          and av.deleted = false
          and attr.deleted = false
          and attr.identifier in :attrIds
        order by a.id asc, attr.identifier asc, av.valueIndex asc
    """)
    List<ArticleAvExportRow> findExportRows(
            @Param("articleIds") Collection<Long> articleIds,
            @Param("attrIds") Collection<String> attributeIdentifiers,
            @Param("client") Integer client
    );
}

