package com.song.my_pim.service.exportjob;

import com.song.my_pim.dto.exportjob.ArticleAvExportRow;
import com.song.my_pim.dto.exportjob.ArticleExportDto;
import com.song.my_pim.dto.exportjob.AttributeValueDto;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public final class ArticleExportMapper {
    private ArticleExportMapper(){}

    public static List<ArticleExportDto> groupByArticle(List<ArticleAvExportRow> rows) {
        if (log.isDebugEnabled()) {
            log.debug("groupByArticle method starts: rows = {}", rows == null ? 0 : rows.size());
        }

        // LinkedHashMap can ensure the order of article
        Map<Long, ArticleExportDto> map = new LinkedHashMap<>();

        for (ArticleAvExportRow articleAvExportRow : rows) {
            ArticleExportDto dto = map.computeIfAbsent(
                    articleAvExportRow.getArticleId(),
                    id -> new ArticleExportDto(id, articleAvExportRow.getArticleNo(), articleAvExportRow.getProductNo(), new LinkedHashMap<>())
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

    private static String toStringValue(ArticleAvExportRow r) {

        String text = r.getValueText();
        if (text != null) return text;

        if (r.getValueNum() != null) return r.getValueNum().toPlainString();
        if (r.getValueBool() != null) return r.getValueBool().toString();
        if (r.getValueDate() != null) return r.getValueDate().toString();

        return null;
    }
}
