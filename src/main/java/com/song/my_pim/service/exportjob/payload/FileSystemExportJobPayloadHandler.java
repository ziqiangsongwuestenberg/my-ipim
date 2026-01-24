package com.song.my_pim.service.exportjob.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.service.exportjob.process.ExportJobContext;
import com.song.my_pim.service.exportjob.process.ExportJobResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemExportJobPayloadHandler implements ExportJobPayloadHandler {

    private final ObjectMapper objectMapper ;

    @Override
    public void init(ExportJobContext exportJobContext) {
        try {
            Files.createDirectories(exportJobContext.getPayloadDir());
            Files.createDirectories(exportJobContext.getPayloadDir().resolve(ExportConstants.EXPORT));
            Files.createDirectories(exportJobContext.getPayloadDir().resolve("export/parts"));
            Files.createDirectories(exportJobContext.getPayloadDir().resolve("transfer"));
            Files.createDirectories(exportJobContext.getPayloadDir().resolve("logs"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to init payload dir: " + exportJobContext.getPayloadDir(), e);
        }
    }

    @Override
    public Path getExportFilePath(ExportJobContext exportJobContext) {
        String prefix = (exportJobContext.getFilePrefix() == null || exportJobContext.getFilePrefix().isBlank())
                ? ExportConstants.EXPORT
                : exportJobContext.getFilePrefix();
        return exportJobContext.getPayloadDir().resolve(ExportConstants.EXPORT).resolve(prefix + ".xml");
    }

    @Override
    public Path getPartFilePath(ExportJobContext exportJobContext, int threadNo) {
        String name = String.format("part-%03d.xml", threadNo);
        return exportJobContext.getPayloadDir().resolve("export/parts").resolve(name);
    }

    @Override
    public void writeMeta(ExportJobContext exportJobContext) {
        try {
            Path metaFile = exportJobContext.getPayloadDir().resolve("meta.json");
            Files.writeString(metaFile,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportJobContext),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.warn("Failed to write meta.json", e);
        }
    }

    @Override
    public void writeError(ExportJobContext exportJobContext, String name, String content) {
        try {
            Path f = exportJobContext.getPayloadDir().resolve("logs").resolve(name);
            Files.writeString(f, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.warn("Failed to write error payload", e);
        }
    }

    @Override
    public void writeSummary(ExportJobContext exportJobContext, ExportJobResult result) {
        try {
            Path f = exportJobContext.getPayloadDir().resolve("logs").resolve("summary.json");
            Files.writeString(f,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            log.warn("Failed to write summary.json", e);
        }
    }

    public static String buildJobFolderName(String jobId) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return ts + "_job-" + jobId;
    }
}
