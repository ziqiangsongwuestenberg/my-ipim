package com.song.my_pim.dto.export;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttributeValueDto {

    private String value;

    private String unit;

    public AttributeValueDto() {
    }

    public AttributeValueDto(String value, String unit) {
        this.value = value;
        this.unit = unit;
    }
}
