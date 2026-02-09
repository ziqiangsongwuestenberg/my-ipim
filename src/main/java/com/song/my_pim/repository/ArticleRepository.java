package com.song.my_pim.repository;

import com.song.my_pim.entity.article.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    Optional<Article> findByArticleNo(String articleNo);

    List<Article> findByProductNo(String productNo);

    List<Article> findByClientAndDeletedFalse(Integer client);
    List<Article> findByClientAndIdInAndDeletedFalse(Integer client, List<Long> ids);

    @Query("select a.id from Article a where a.client = :client and a.deleted = false")
    List<Long> findIdsByClientAndDeletedFalse(@Param("client") Integer client);
}

