package com.song.my_pim.dto.exportjob;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ArticlePriceExportRow {

    private Long articleId;
    private String articleNo;
    private String priceIdentifier;
    private BigDecimal amount;
    private String currency;
    private LocalDate validFrom;

    public ArticlePriceExportRow() {
        // for JPA / Jackson
    }

    public ArticlePriceExportRow(Long articleId,
                                 String articleNo,
                                 String priceIdentifier,
                                 BigDecimal amount,
                                 String currency,
                                 LocalDate validFrom) {
        this.articleId = articleId;
        this.articleNo = articleNo;
        this.priceIdentifier = priceIdentifier;
        this.amount = amount;
        this.currency= currency;
        this.validFrom = validFrom;
    }
}

