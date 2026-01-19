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
import com.song.my_pim.service.exportjob.writer.XmlExportWithAttributesAndPricesWriter;
import com.song.my_pim.specification.ArticleExportToXMLFileSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
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

    @Transactional(readOnly = true)
    public void exportToXml(Integer client, ArticleExportRequest request, OutputStream outputStream) {
        //todo : use specification
        Specification specification = ArticleExportToXMLFileSpecification.build(props, client, request.getIncludeDeleted());
        List<Article> articles = articleRepository.findByClientAndDeletedFalse(client);
        //todo : here can filter Articles by article statues
        List<Long> articeIds = articles.stream().map(Article::getId).toList();
        // 1, get ArticleAvExportRow and ArticlePriceExportRow
        Map<Long, List<ArticleAvExportRow>> attrsByArticleIdMap = loadArticleAVForExport(client, articeIds);
        Map<Long, List<ArticlePriceExportRow>> priceByArticleIdMap = loadpriceForExport(client, articeIds);
        // 2, mapper ArticleAvExportRow/ArticlePriceExportRow -> List<ArticleExportDto>
        List<ArticleExportDto> articleExportDTOList = ArticleExportMapper.groupArticlesWithAttributesAndPrices(articles, attrsByArticleIdMap, priceByArticleIdMap);
        // 3, write xml
        try {
            xmlExportWithAttributesAndPricesWriter.write(articleExportDTOList, outputStream);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e); //todo : change here
        }
    }

    private Map<Long, List<ArticleAvExportRow>> loadArticleAVForExport(Integer client, List<Long> articeIds){
        Map<Long, List<ArticleAvExportRow>> attrsByArticleId =
                articleAvRepository.findExportRows(articeIds, props.getAttributeWhitelist(),client)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ArticleAvExportRow::getArticleId
                        ));
        return attrsByArticleId;
    }

    private Map<Long, List<ArticlePriceExportRow>> loadpriceForExport(Integer client, List<Long> articeIds){
        Map<Long, List<ArticlePriceExportRow>> pricesByArticleId =
                articlePriceRelRepository.findPriceExportRows(articeIds, client)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ArticlePriceExportRow::getArticleId
                        ));
        return pricesByArticleId;
    }

}
