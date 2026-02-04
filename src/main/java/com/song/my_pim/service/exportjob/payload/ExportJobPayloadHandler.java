package com.song.my_pim.service.exportjob.payload;

import com.song.my_pim.service.exportjob.process.ExportJobContext;
import com.song.my_pim.service.exportjob.process.ExportJobResult;

import java.nio.file.Path;

public interface ExportJobPayloadHandler {

    void init(ExportJobContext exportJobContext);

    Path getExportFilePath(ExportJobContext exportJobContext);                 // final export.xml
    Path getPartFilePath(ExportJobContext exportJobContext, int threadNo);     // part-001.xml

    void writeMeta(ExportJobContext exportJobContext);
    void writeError(ExportJobContext exportJobContext, String name, String content);
    void writeError(ExportJobContext exportJobContext, String name, Throwable ex); //for stack traces
    void writeSummary(ExportJobContext exportJobContext, ExportJobResult result);
}
