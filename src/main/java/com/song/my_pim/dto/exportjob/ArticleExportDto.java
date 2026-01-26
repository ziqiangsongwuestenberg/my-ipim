package com.song.my_pim.dto.exportjob;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ArticleExportDto {

    private Long articleId;
    private String productType;   // product / article
    private String articleNo;
    private String productNo;

    private Map<String, AttributeValueDto> attributes = new LinkedHashMap<>();// same identifier appears multiple times -> Use "|" to concatenate them.
    private Map<String, ArticlePriceExportDto> prices = new LinkedHashMap<>();

    public ArticleExportDto(Long articleId, String articleNo, String productNo, Map<String, AttributeValueDto> attributes, Map<String, ArticlePriceExportDto> prices) {
        this.articleId = articleId;
        this.articleNo = articleNo;
        this.productNo = productNo;
        this.attributes = attributes;
        this.prices = prices;
    }

    public boolean isArticle() {
        return "article".equalsIgnoreCase(productType);
    }

    public boolean isProduct() {
        return "product".equalsIgnoreCase(productType);
    }
}
