package com.song.my_ipim.repository;

import com.song.my_ipim.entity.article.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    Optional<Article> findByArticleNo(String articleNo);

    List<Article> findByProductNo(String productNo);
}

