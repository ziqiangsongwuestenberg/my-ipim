package com.song.my_pim.dto.exportjob;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class ArticlePriceExportDto {

    private String priceIdentifier;
    private BigDecimal amount;
    private LocalDate validFrom;
    private String currency;

    public static ArticlePriceExportDto from(ArticlePriceExportRow row) {
        ArticlePriceExportDto dto = new ArticlePriceExportDto();
        dto.setPriceIdentifier(row.getPriceIdentifier());
        dto.setAmount(row.getAmount());
        dto.setCurrency(row.getCurrency());
        dto.setValidFrom(row.getValidFrom());
        return dto;
    }
}
