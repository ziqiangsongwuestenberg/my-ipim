package com.song.my_pim.service.exportjob.mapper;

import com.song.my_pim.dto.exportjob.*;
import com.song.my_pim.entity.article.Article;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public final class ArticleExportMapper {
    private ArticleExportMapper(){}

    // mapper 1
    public static List<ArticleExportDto> groupArticlesWithAttributes(List<ArticleAvExportRow> rows) {
        if (log.isDebugEnabled()) {
            log.debug("groupByArticle method starts: rows = {}", rows == null ? 0 : rows.size());
        }

        // LinkedHashMap can ensure the order of article
        Map<Long, ArticleExportDto> map = new LinkedHashMap<>();

        for (ArticleAvExportRow articleAvExportRow : rows) {
            ArticleExportDto dto = map.computeIfAbsent(
                    articleAvExportRow.getArticleId(),
                    id -> new ArticleExportDto(id,
                            articleAvExportRow.getArticleNo(),
                            articleAvExportRow.getProductNo(),
                            new LinkedHashMap<>(), // attributes
                            new LinkedHashMap<>()) // prices
            );

            String value = toStringValue(articleAvExportRow);
            if (value == null || value.isBlank()) continue;

            AttributeValueDto valueDto = new AttributeValueDto(value, articleAvExportRow.getUnit());

            // same identifier appears multiple time -> Use "|" to concatenate them.
            dto.getAttributes().merge(
                    articleAvExportRow.getAttributeIdentifier(),
                    valueDto,
                    (oldV, newV) -> {
                        oldV.setValue(oldV.getValue() + "|" + newV.getValue());

                        // unit
                        if (oldV.getUnit() == null && newV.getUnit() != null) {
                            oldV.setUnit(newV.getUnit());
                        }
                        return oldV;
                    }
            );
        }

        List<ArticleExportDto> res = new ArrayList<>(map.values());

        if (log.isDebugEnabled()) {
            log.debug("groupByArticle method done: articles = {}", res.size());
        }
        return res;
    }

    // mapper 2
    public static List<ArticleExportDto> groupArticlesWithAttributesAndPrices(
            List<Article> articles,
            Map<Long, List<ArticleAvExportRow>> attrsByArticleIdMap,
            Map<Long, List<ArticlePriceExportRow>> priceByArticleIdMap
    ){
        List<ArticleExportDto> articleExportDtoList = new ArrayList<>();

        for (Article a : articles) {
            ArticleExportDto dto = new ArticleExportDto();
            dto.setArticleId(a.getId());
            dto.setProductType(a.getArticleType().toLowerCase());
            dto.setArticleNo(a.getArticleNo());
            dto.setProductNo(a.getProductNo());

            //1,add attribute
            for(ArticleAvExportRow row : attrsByArticleIdMap.get(a.getId())){
                String value = toStringValue(row);
                if (value == null || value.isBlank()) continue;
                AttributeValueDto valueDto = new AttributeValueDto(value, row.getUnit());
                dto.getAttributes().merge(
                        row.getAttributeIdentifier(),
                        valueDto,
                        (oldV, newV) -> {
                            oldV.setValue(oldV.getValue() + "|" + newV.getValue());

                            // unit
                            if (oldV.getUnit() == null && newV.getUnit() != null) {
                                oldV.setUnit(newV.getUnit());
                            }
                            return oldV;
                        }
                );
            }

            //2,add price
            if (dto.isArticle()) {
                for(ArticlePriceExportRow row : priceByArticleIdMap.get(a.getId())) {
                    dto.getPrices().put(
                            row.getPriceIdentifier(),
                            ArticlePriceExportDto.from(row)
                            );
                }
            }
            articleExportDtoList.add(dto);
        }
        return articleExportDtoList;
    }

    private static String toStringValue(ArticleAvExportRow r) {

        String text = r.getValueText();
        if (text != null) return text;

        if (r.getValueNum() != null) return r.getValueNum().toPlainString();
        if (r.getValueBool() != null) return r.getValueBool().toString();
        if (r.getValueDate() != null) return r.getValueDate().toString();

        return null;
    }
}
