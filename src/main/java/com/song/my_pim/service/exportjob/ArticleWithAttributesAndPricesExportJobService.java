package com.song.my_pim.service.exportjob;

import com.song.my_pim.config.ExportJobProperties;
import com.song.my_pim.dto.exportjob.ArticleAvExportRow;
import com.song.my_pim.dto.exportjob.ArticleExportDto;
import com.song.my_pim.dto.exportjob.ArticleExportRequest;
import com.song.my_pim.dto.exportjob.ArticlePriceExportRow;
import com.song.my_pim.entity.article.Article;
import com.song.my_pim.repository.ArticleAvRepository;
import com.song.my_pim.repository.ArticlePriceRelRepository;
import com.song.my_pim.repository.ArticleRepository;
import com.song.my_pim.service.exportjob.mapper.ArticleExportMapper;
import com.song.my_pim.service.exportjob.s3Service.ExportToS3Service;
import com.song.my_pim.service.exportjob.writer.XmlExportWithAttributesAndPricesWriter;
import com.song.my_pim.specification.ArticleExportToXMLFileSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleWithAttributesAndPricesExportJobService implements XmlExportJob{

    private final ExportJobProperties props;
    private final XmlExportWithAttributesAndPricesWriter xmlExportWithAttributesAndPricesWriter;
    private final ArticleRepository articleRepository;
    private final ArticleAvRepository articleAvRepository;
    private final ArticlePriceRelRepository articlePriceRelRepository;
    private final ArticleExportMapper articleExportMapper;
    private final ExportToS3Service s3ExportService;
    @Autowired
    private ApplicationContext applicationContext;

    @Transactional(readOnly = true)
    public void exportToXml(Integer client, ArticleExportRequest request, OutputStream outputStream) {
        //todo : use specification
        Specification specification = ArticleExportToXMLFileSpecification.build(props, client, request.getIncludeDeleted());
        List<Article> articles = articleRepository.findAll(specification);
        List<Long> articeIds = articles.stream().map(Article::getId).toList();
        // 1, get ArticleAvExportRow and ArticlePriceExportRow
        Map<Long, List<ArticleAvExportRow>> attrsByArticleIdMap = loadArticleAVForExport(client, articeIds);
        Map<Long, List<ArticlePriceExportRow>> priceByArticleIdMap = loadPriceForExport(client, articeIds);
        // 2, mapper ArticleAvExportRow/ArticlePriceExportRow -> List<ArticleExportDto>
        List<ArticleExportDto> articleExportDTOList = articleExportMapper.groupArticlesWithAttributesAndPrices(articles, attrsByArticleIdMap, priceByArticleIdMap);
        // 3, write xml
        try {
            xmlExportWithAttributesAndPricesWriter.write(articleExportDTOList, outputStream);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e); //todo : change here
        }
    }

    @Transactional(readOnly = true)
    public String exportArticlesXmlToS3(Integer client, ArticleExportRequest request) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("articles-export-", ".xml");

            try (OutputStream out = Files.newOutputStream(tmp)) {
                // Calling @Transactional exportToXml method within the same class bypasses Spring proxy (self-invocation).
                ArticleWithAttributesAndPricesExportJobService service = applicationContext.getBean(ArticleWithAttributesAndPricesExportJobService.class);
                service.exportToXml(client, request, out);
            }

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
            String ts = LocalDateTime.now().format(fmt);
            String key = "client-" + client + "/articles-" + ts + ".xml";
            return s3ExportService.uploadXmlFile(tmp, key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export/upload xml", e);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        }
    }

    private Map<Long, List<ArticleAvExportRow>> loadArticleAVForExport(Integer client, List<Long> articleIds){
        List<String> whitelist = props.getAttributeWhitelist();
        if (whitelist == null || whitelist.isEmpty() || articleIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ArticleAvExportRow>> attrsByArticleId =
                articleAvRepository.findExportRows(articleIds, whitelist, client)
                        .stream().collect(Collectors.groupingBy(ArticleAvExportRow::getArticleId));
        return attrsByArticleId;
    }

    private Map<Long, List<ArticlePriceExportRow>> loadPriceForExport(Integer client, List<Long> articleIds){
        List<String> whitelist = props.getPriceWhitelist();
        if (whitelist == null || whitelist.isEmpty() || articleIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ArticlePriceExportRow>> pricesByArticleId =
                articlePriceRelRepository.findPriceExportRows(articleIds, whitelist, client)
                        .stream().collect(Collectors.groupingBy(ArticlePriceExportRow::getArticleId));
        return pricesByArticleId;
    }

}
