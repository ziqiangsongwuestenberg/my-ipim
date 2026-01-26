package com.song.my_pim.dto.exportjob;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ArticleAvExportRow {

    private Long articleId;
    private String articleNo;
    private String productNo;

    private String attributeIdentifier;
    private String valueType;
    private String unit;
    private Integer valueIndex;

    private String valueText;
    private BigDecimal valueNum;
    private Boolean valueBool;
    private LocalDate valueDate;

    public ArticleAvExportRow(
            Long articleId,
            String articleNo,
            String productNo,
            String attributeIdentifier,
            String valueType,
            String unit,
            Integer valueIndex,
            String valueText,
            BigDecimal valueNum,
            Boolean valueBool,
            LocalDate valueDate
    ) {
        this.articleId = articleId;
        this.articleNo = articleNo;
        this.productNo = productNo;
        this.attributeIdentifier = attributeIdentifier;
        this.valueType = valueType;
        this.unit = unit;
        this.valueIndex = valueIndex;
        this.valueText = valueText;
        this.valueNum = valueNum;
        this.valueBool = valueBool;
        this.valueDate = valueDate;
    }

    /**
     * Export helper:
     * returns the first non-null value as string
     */
    public String getValueAsString() {
        if (valueText != null) {
            return valueText;
        }
        if (valueNum != null) {
            return valueNum.toPlainString();
        }
        if (valueBool != null) {
            return valueBool.toString();
        }
        if (valueDate != null) {
            return valueDate.toString();
        }
        return "";
    }
}
