package com.song.my_pim.service.exportjob;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.config.ExportJobProperties;
import com.song.my_pim.dto.exportjob.ArticleExportDto;
import com.song.my_pim.dto.exportjob.ArticleExportRequest;
import com.song.my_pim.entity.article.Article;
import com.song.my_pim.repository.ArticleAvRepository;
import com.song.my_pim.repository.ArticleRepository;
import com.song.my_pim.specification.ArticleExportToXMLFileSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleExportJobService {

    private final ExportJobProperties props;
    private final ArticleRepository articleRepo;
    private final ArticleAvRepository avRepo;
    private final XmlExportWriter xmlWriter;

    @Transactional(readOnly = true)
    public void exportToXml(Integer client, ArticleExportRequest request, OutputStream outputStream) {
        var spec = ArticleExportToXMLFileSpecification.build(props, client, request.getIncludeDeleted());

        int pageSize = Math.max(1, props.getPageSize());
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.ASC, ExportConstants.ID));


        List<ArticleExportDto> all = new ArrayList<>();

        Page<Article> page;
        do {
            page = articleRepo.findAll(spec, pageable);

            var articleIds = page.getContent().stream().map(Article::getId).toList();
            if (!articleIds.isEmpty()) {
                var rows = avRepo.findExportRows(articleIds, props.getAttributeWhitelist(), client);
                all.addAll(ArticleExportMapper.groupByArticle(rows));
            }

            pageable = page.hasNext() ? page.nextPageable() : Pageable.unpaged();
        } while (page.hasNext());

        log.info("Get {} articles that meet the export criteria", all.size());
        xmlWriter.write(outputStream, client, all);
    }
}
