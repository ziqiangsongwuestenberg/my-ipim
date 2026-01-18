package com.song.my_pim.dto.categoryNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CategoryNodeDto {
    private Long id;
    private String identifier;
    private String name;
    private List<CategoryNodeDto> children = new ArrayList<>();
}

