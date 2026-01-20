package com.song.my_pim.service.exportjob.mapper;

import com.song.my_pim.common.util.ExportNumberFormatter;
import com.song.my_pim.dto.exportjob.*;
import com.song.my_pim.entity.article.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public final class ArticleExportMapper {
    private ArticleExportMapper(){}

    // mapper 1
    public List<ArticleExportDto> groupArticlesWithAttributes(List<ArticleAvExportRow> rows) {
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

            mergeAttributeValue(articleAvExportRow, dto);
        }

        List<ArticleExportDto> res = new ArrayList<>(map.values());

        if (log.isDebugEnabled()) {
            log.debug("groupByArticle method done: articles = {}", res.size());
        }
        return res;
    }

    // mapper 2
    public List<ArticleExportDto> groupArticlesWithAttributesAndPrices(
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
            for(ArticleAvExportRow articleAvExportRow : attrsByArticleIdMap.get(a.getId())){
                mergeAttributeValue(articleAvExportRow, dto);
            }

            //2,add price
            if (dto.isArticle()) {
                for(ArticlePriceExportRow articleAvExportRow : priceByArticleIdMap.get(a.getId())) {
                    dto.getPrices().put(
                            articleAvExportRow.getPriceIdentifier(),
                            ArticlePriceExportDto.from(articleAvExportRow)
                            );
                }
            }
            articleExportDtoList.add(dto);
        }
        return articleExportDtoList;
    }

    private void mergeAttributeValue(ArticleAvExportRow row, ArticleExportDto dto) {

        String identifier = row.getAttributeIdentifier();
        if (identifier == null || identifier.isBlank()) return;

        AttributeValueDto newV = toAttributeValueDto(row);
        if (newV == null || newV.getValue() == null || newV.getValue().isBlank()) return;

        dto.getAttributes().merge(identifier, newV, (oldV, incoming) -> {

            if (oldV.getUnit() == null && incoming.getUnit() != null) {
                oldV.setUnit(incoming.getUnit());
            }

            if (isTextType(row)) {
                oldV.setValue(oldV.getValue() + "|" + incoming.getValue());
                return oldV;
            }

            // log.warn("Duplicate attribute '{}' for articleId={}, non-text type={}, keep first value",
            //          identifier, row.getArticleId(), row.getValueType());

            return oldV;
        });
    }

    private boolean isTextType(ArticleAvExportRow row) {
        return "TEXT".equalsIgnoreCase(row.getValueType());
    }

    private AttributeValueDto toAttributeValueDto(ArticleAvExportRow row) {

        String v = switch (row.getValueType().toUpperCase()) {
            case "STRING"  -> row.getValueText();
            case "NUMBER"  -> ExportNumberFormatter.decimal2(row.getValueNum());
            case "BOOLON" -> row.getValueBool() == null ? null : row.getValueBool().toString();
            case "DATE" -> row.getValueDate() == null ? null : row.getValueDate().toString();
            default     -> null;
        };

        if (v == null || v.isBlank()){
            log.warn(
                "Skip attribute value: articleId={}, attribute={}, valueType={}, reason=empty_or_unsupported",
                row.getArticleId(),
                row.getAttributeIdentifier(),
                row.getValueType()
            );
            return null;
        }
        return new AttributeValueDto(v, row.getUnit());
    }

}
