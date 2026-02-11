package com.song.my_pim.service.exportjob;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.common.exception.ExportJobInitException;
import com.song.my_pim.common.exception.ExportWriteException;
import com.song.my_pim.config.ExportJobProperties;
import com.song.my_pim.dto.exportjob.ArticleExportRequest;
import com.song.my_pim.monitoring.ExportJobMetrics;
import com.song.my_pim.repository.ArticleRepository;
import com.song.my_pim.service.exportjob.chunk.ArticleChunkExportService;
import com.song.my_pim.service.exportjob.payload.ExportJobPayloadHandler;
import com.song.my_pim.service.exportjob.payload.FileSystemExportJobPayloadHandler;
import com.song.my_pim.service.exportjob.process.ExportJobContext;
import com.song.my_pim.service.exportjob.process.ExportJobResult;
import com.song.my_pim.service.exportjob.process.ExportJobUtil;
import com.song.my_pim.service.exportjob.s3Service.ExportToS3Service;
import com.song.my_pim.service.exportjob.writer.asyncWriter.XmlExportStreamWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleAsyncExportJobService implements XmlExportJob{

    private final ApplicationContext applicationContext;
    private final ExportJobProperties exportJobProperties;
    private final ExportJobPayloadHandler payloadHandler;
    private final ArticleRepository articleRepository;
    private final ArticleChunkExportService articleChunkExportService;
    private final ExportToS3Service s3ExportService;
    private final XmlExportStreamWriter xmlExportStreamWriter;
    private final ExportJobMetrics exportJobMetrics;


    public void exportToXml(Integer client, ArticleExportRequest request, OutputStream outputStream) {

        exportJobMetrics.time("export", "articles_xml", String.valueOf(client), () -> {
            ExportJobContext exportJobContext = buildContext(client, request);
            ChunkRunResult chunkRunResult = null;


            try {
                exportJobContext.getPayloadHandler().init(exportJobContext);
                exportJobContext.getPayloadHandler().writeMeta(exportJobContext);

                List<Long> articleIds = articleRepository.findIdsByClientAndDeletedFalse(client);

                // Async export
                chunkRunResult = exportChunksAsync(exportJobContext, articleIds);
                if (chunkRunResult.mergedResult.getFailed() > 0) {
                    throw new ExportWriteException("Export failed in one or more chunks. jobId=" + exportJobContext.getJobId());
                }

                // merge
                Path finalFile = mergePartsToFinalXml(exportJobContext, chunkRunResult.partCount);

                // stream to response out
                try (InputStream inputStream = Files.newInputStream(finalFile)) {
                    inputStream.transferTo(outputStream);
                } catch (IOException e) {
                    throw new ExportWriteException("Failed to stream export file", e);
                }

            } catch (ExportJobInitException e) {
                log.error("Export init failed. jobId={}, client={}, payloadDir={}",
                        exportJobContext.getJobId(), exportJobContext.getClient(), exportJobContext.getPayloadDir(), e);
                throw e;
            } finally {
                try {
                    if (chunkRunResult != null) {
                        exportJobContext.getPayloadHandler().writeSummary(exportJobContext, chunkRunResult.mergedResult);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to write export summary. jobId={}", exportJobContext.getJobId(), ex);
                }
            }
        });
    }

    private record ChunkRunResult(int partCount, ExportJobResult mergedResult) {}

    private ChunkRunResult exportChunksAsync(ExportJobContext exportJobContext, List<Long> articleIds){
        List<List<Long>> partitions = ExportJobUtil.partition(articleIds, exportJobContext.getChunkParts());

        List<CompletableFuture<ExportJobResult>> futures = new ArrayList<>();
        int threadNo = 0;
        for (List<Long> chunk : partitions) {
            threadNo++;
            futures.add(articleChunkExportService.exportChunkAsync(exportJobContext, chunk, threadNo));
        }

        ExportJobResult merged = ExportJobUtil.waitAllAndMerge(futures, ExportJobResult.empty());

        return new ChunkRunResult(partitions.size(), merged);
    }

    private ExportJobContext buildContext(Integer client, ArticleExportRequest request) {
        String jobId = UUID.randomUUID().toString();

        Path baseDir = exportJobProperties.getPayload().getBaseDir();
        Path payloadDir = baseDir.resolve(FileSystemExportJobPayloadHandler.buildJobFolderName(jobId));

        ExportJobContext exportJobContext = ExportJobContext.builder()
                .jobId(jobId)
                .client(client)
                .chunkParts(exportJobProperties.getChunkParts() > 0 ? exportJobProperties.getChunkParts() : 4)
                .filePrefix("articles-export")
                .startTime(Instant.now())
                .payloadDir(payloadDir)
                .payloadHandler(payloadHandler)
                .build();

        exportJobContext.putAttr(ExportConstants.ATTR_REQUEST, request);
        return exportJobContext;
    }

    private Path mergePartsToFinalXml(ExportJobContext exportJobContext, int partCount) {

        Path finalFilePath = exportJobContext.getPayloadHandler().getExportFilePath(exportJobContext);

        try (OutputStream out = Files.newOutputStream(
                finalFilePath,
                StandardOpenOption.CREATE, // if not exist, create a new one
                StandardOpenOption.TRUNCATE_EXISTING // id exist, clean and write again
        )) {

            // 1, write the head of XML : <export>
            XMLStreamWriter xmlWriter = xmlExportStreamWriter.start(out);
            xmlWriter.flush(); //before OutputStream(copy)

            // 2, assemble the part files in threadNo order.
            concatenateArticlePartXml(exportJobContext, partCount, out);

            // 3, write the end of XML: </export>
            xmlExportStreamWriter.finish(xmlWriter);

        } catch (Exception e) {
            exportJobContext.getPayloadHandler().writeError(
                    exportJobContext,
                    "merge-error.txt",
                    "Failed to merge parts: " + e.getMessage()
            );
            throw new ExportWriteException("Failed to merge XML part files", e);
        }

        // record the final file path in the context
        exportJobContext.setExportFile(finalFilePath);
        return finalFilePath;
    }

    private void concatenateArticlePartXml(ExportJobContext exportJobContext, int partCount, OutputStream out) throws IOException {
        for (int i = 1; i <= partCount; i++) {
            Path partFile = exportJobContext.getPayloadHandler().getPartFilePath(exportJobContext, i);

            if (!Files.exists(partFile)) {
                // if a chunk fail
                exportJobContext.getPayloadHandler().writeError(
                        exportJobContext,
                        "merge-missing-part-" + i + ".txt",
                        "Missing part file: " + partFile
                );
                continue;
            }

            // copy the partFile !
            Files.copy(partFile, out);
        }
    }

    public String exportArticlesXmlToS3(Integer client, ArticleExportRequest request) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("articles-export-", ".xml");
            final Path tmpFinal = tmp; // tmp in lambda has to be final

            try (OutputStream out = Files.newOutputStream(tmpFinal)) {
                // Calling @Transactional exportToXml method within the same class bypasses Spring proxy (self-invocation).
                ArticleAsyncExportJobService service = applicationContext.getBean(ArticleAsyncExportJobService.class);
                service.exportToXml(client, request, out);
            }

            String key = "client-" + client + "/articles-" + java.time.LocalDateTime.now() + ".xml";

            String s3rui = exportJobMetrics.time("upload", "articles_xml", String.valueOf(client), () -> s3ExportService.uploadXmlFile(tmpFinal, key));

            return s3rui;
        } catch (IOException e) {
            throw new ExportWriteException("Failed to export/upload xml", e);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        }
    }

}
