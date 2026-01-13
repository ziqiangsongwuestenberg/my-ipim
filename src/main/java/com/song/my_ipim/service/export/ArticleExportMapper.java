package com.song.my_ipim.service.export;

import com.song.my_ipim.dto.export.ArticleAvExportRow;
import com.song.my_ipim.dto.export.ArticleExportDto;

import java.util.*;

public final class ArticleExportMapper {
    private ArticleExportMapper(){}

    public static List<ArticleExportDto> groupByArticle(List<ArticleAvExportRow> rows) {
        // LinkedHashMap can ensure the order of article
        Map<Long, ArticleExportDto> map = new LinkedHashMap<>();

        for (ArticleAvExportRow r : rows) {
            ArticleExportDto dto = map.computeIfAbsent(
                    r.getArticleId(),
                    id -> new ArticleExportDto(id, r.getArticleNo(), r.getProductNo(), new LinkedHashMap<>())
            );

            String value = toStringValue(r);
            if (value == null || value.isBlank()) continue;

            // same identifier appears multiple time -> Use "|" to concatenate them.
            dto.getAttributes().merge(r.getAttributeIdentifier(), value, (oldV, newV) -> oldV + "|" + newV);
        }

        return new ArrayList<>(map.values());
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
