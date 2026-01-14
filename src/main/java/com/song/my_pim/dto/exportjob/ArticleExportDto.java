package com.song.my_pim.dto.export;

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
    private String articleNo;
    private String productNo;

    private Map<String, AttributeValueDto> attributes = new LinkedHashMap<>();

    public ArticleExportDto(Long articleId, String articleNo, String productNo, Map<String, AttributeValueDto> attributes) {
        this.articleId = articleId;
        this.articleNo = articleNo;
        this.productNo = productNo;
        this.attributes = attributes;
    }
}
