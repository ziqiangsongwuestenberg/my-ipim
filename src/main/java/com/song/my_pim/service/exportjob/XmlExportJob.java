package com.song.my_pim.service.exportjob;

import com.song.my_pim.dto.exportjob.ArticleExportRequest;

import java.io.OutputStream;

public interface XmlExportJob {
    void exportToXml(Integer client, ArticleExportRequest request, OutputStream out);
}
