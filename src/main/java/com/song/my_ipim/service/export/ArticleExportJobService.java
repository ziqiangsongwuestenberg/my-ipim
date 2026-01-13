package com.song.my_ipim.service.export;

import com.song.my_ipim.config.ExportJobProperties;
import com.song.my_ipim.dto.export.ArticleExportDto;
import com.song.my_ipim.repository.ArticleAvRepository;
import com.song.my_ipim.repository.ArticleRepository;
import com.song.my_ipim.specification.ArticleExportToXMLFileSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleExportJobService {

    private final ExportJobProperties props;
    private final ArticleRepository articleRepo;
    private final ArticleAvRepository avRepo;

    private final XmlExportWriter xmlWriter = new XmlExportWriter();

    @Transactional(readOnly = true)
    public void exportToXml(OutputStream os) {
        var spec = ArticleExportToXMLFileSpecification.build(props);

        int pageSize = Math.max(1, props.getPageSize());
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, "id"));

        // 这里为了“边查边写”，我们用一个小技巧：先写 header，然后每页写一批 article
        // 但我们 XmlExportWriter 目前是一次性写完整文档。
        // 最简单：先把所有 ArticleExportDto 收集到 list（数据量小可行）
        // 数据量大：我们把 XmlExportWriter 改成“分段写”，我下面也告诉你怎么改。

        // 先用“简单版”（适合先跑通）
        List<ArticleExportDto> all = new java.util.ArrayList<>();

        Page<com.song.my_ipim.entity.article.Article> page;
        do {
            page = articleRepo.findAll(spec, pageable);

            var articleIds = page.getContent().stream().map(com.song.my_ipim.entity.article.Article::getId).toList();
            if (!articleIds.isEmpty()) {
                var rows = avRepo.findExportRows(articleIds, props.getAttributeWhitelist(), props.getClient());
                all.addAll(ArticleExportMapper.groupByArticle(rows));
            }

            pageable = page.hasNext() ? page.nextPageable() : Pageable.unpaged();
        } while (page.hasNext());

        xmlWriter.write(os, props.getClient(), all);
    }
}
