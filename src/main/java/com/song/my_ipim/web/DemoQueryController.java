package com.song.my_ipim.web;

import com.song.my_ipim.entity.article.Article;
import com.song.my_ipim.entity.category.CategoryNode;
import com.song.my_ipim.repository.ArticleRepository;
import com.song.my_ipim.repository.CategoryNodeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DemoQueryController {

    private final ArticleRepository articleRepo;
    private final CategoryNodeRepository nodeRepo;

    public DemoQueryController(ArticleRepository articleRepo, CategoryNodeRepository nodeRepo) {
        this.articleRepo = articleRepo;
        this.nodeRepo = nodeRepo;
    }

    @GetMapping("/articles")
    public List<Article> articles() {
        return articleRepo.findAll();
    }

    @GetMapping("/article-count")
    public long articleCount() {
        return articleRepo.count();
    }

    @GetMapping("/articles/{articleNo}")
    public Article byArticleNo(@PathVariable String articleNo) {
        return articleRepo.findByArticleNo(articleNo)
                .orElseThrow(() -> new RuntimeException("article not found: " + articleNo));
    }

    @GetMapping("/nodes/subtree")
    public List<CategoryNode> subtree(@RequestParam Long rootId, @RequestParam String prefix) {
        // prefix sample："/root/cat_a"
        return nodeRepo.findByRootNode_IdAndHierarchyPathStartingWith(rootId, prefix);
    }
}
