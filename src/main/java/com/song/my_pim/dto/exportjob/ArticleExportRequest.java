package com.song.my_pim.dto.exportjob;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleExportRequest {
    private Integer client;
    private Boolean includeDeleted;
}
