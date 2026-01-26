package com.song.my_pim.service.exportjob.chunk;

import com.song.my_pim.config.ExportJobProperties;
import com.song.my_pim.dto.exportjob.ArticleExportDto;
import com.song.my_pim.dto.exportjob.ArticleAvExportRow;
import com.song.my_pim.dto.exportjob.ArticlePriceExportRow;
import com.song.my_pim.entity.article.Article;
import com.song.my_pim.service.exportjob.mapper.ArticleExportMapper;
import com.song.my_pim.repository.ArticleAvRepository;
import com.song.my_pim.repository.ArticlePriceRelRepository;
import com.song.my_pim.repository.ArticleRepository;
import com.song.my_pim.service.exportjob.process.ExportJobContext;
import com.song.my_pim.service.exportjob.process.ExportJobResult;
import com.song.my_pim.service.exportjob.writer.asyncWriter.XmlExportPartWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleChunkExportService {

    public static final String ATTR_REQUEST = "request";

    private final ExportJobProperties props;
    private final ArticleRepository articleRepository;
    private final ArticleAvRepository articleAvRepository;
    private final ArticlePriceRelRepository articlePriceRelRepository;
    private final ArticleExportMapper articleExportMapper;
    @Qualifier("exportJobExecutor")
    private final TaskExecutor exportJobExecutor;

    private final XmlExportPartWriter partWriter = new XmlExportPartWriter();

    public CompletableFuture<ExportJobResult> exportChunkAsync(
            ExportJobContext exportJobContext,
            List<Long> articleIdsChunk,
            int threadNo
    ) {
        return CompletableFuture.supplyAsync(
                () -> exportChunkTx(exportJobContext, articleIdsChunk, threadNo),
                runnable -> exportJobExecutor.execute(runnable)
        );
    }

    @Transactional(readOnly = true)
    protected ExportJobResult exportChunkTx(ExportJobContext exportJobContext, List<Long> articleIdsChunk, int threadNo) {
        long t0 = System.currentTimeMillis();
        ExportJobResult result = ExportJobResult.empty();

//        ArticleExportRequest request = exportJobContext.getAttr(ATTR_REQUEST, ArticleExportRequest.class);
        try {
            Map<Long, List<ArticleAvExportRow>> attrsByArticleIdMap = loadArticleAVForExport(exportJobContext.getClient(), articleIdsChunk);

            Map<Long, List<ArticlePriceExportRow>> priceByArticleIdMap = loadpriceForExport(exportJobContext.getClient(), articleIdsChunk);

            List<Article> articles = articleRepository.findByClientAndIdInAndDeletedFalse(exportJobContext.getClient(), articleIdsChunk);
            List<ArticleExportDto> articleExportDTOList = articleExportMapper.groupArticlesWithAttributesAndPrices(articles, attrsByArticleIdMap, priceByArticleIdMap);

            Path partFile = exportJobContext.getPayloadHandler().getPartFilePath(exportJobContext, threadNo);
            try (OutputStream outputStream = Files.newOutputStream(partFile)) {
                partWriter.writeArticlesFragment(articleExportDTOList, outputStream);
            }

            result.setExported(articleExportDTOList.size());
            log.info("Chunk {} done. exported={}, ms={}", threadNo, articleExportDTOList.size(), System.currentTimeMillis() - t0);
            return result;

        } catch (XMLStreamException ex) {
            log.error("Chunk {} XML write failed", threadNo, ex);
            result.setFailed(result.getFailed() + 1);
            result.getErrors().add("Chunk " + threadNo + " XML write failed: " + ex.getMessage());
            exportJobContext.getPayloadHandler().writeError(exportJobContext, "chunk-" + threadNo + "-xml-error.txt", ex.toString());
            return result;
        } catch (Exception ex) {
            log.error("Chunk {} failed", threadNo, ex);
            result.setFailed(result.getFailed() + 1);
            result.getErrors().add("Chunk " + threadNo + " failed: " + ex.getMessage());
            exportJobContext.getPayloadHandler().writeError(exportJobContext, "chunk-" + threadNo + "-error.txt", ex.toString());
            return result;
        }
    }

    private Map<Long, List<ArticleAvExportRow>> loadArticleAVForExport(Integer client, List<Long> articeIds){
        return articleAvRepository.findExportRows(articeIds, props.getAttributeWhitelist(),client)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ArticleAvExportRow::getArticleId
                        ));
    }

    private Map<Long, List<ArticlePriceExportRow>> loadpriceForExport(Integer client, List<Long> articeIds){
        return articlePriceRelRepository.findPriceExportRows(articeIds, props.getPriceWhitelist(), client)
                        .stream()
                        .collect(Collectors.groupingBy(
                                ArticlePriceExportRow::getArticleId
                        ));
    }
}

