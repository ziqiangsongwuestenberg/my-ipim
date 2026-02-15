package com.song.my_pim.service.exportjob.chunk;

import com.song.my_pim.service.exportjob.process.ExportJobContext;
import com.song.my_pim.service.exportjob.process.ExportJobResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleChunkExportService {

    private final ArticleChunkExportTransactionalService articleChunkExportTransactionalService;

    @Qualifier("exportJobThreadPool")
    private final TaskExecutor exportJobThreadPool;

    public CompletableFuture<ExportJobResult> exportChunkAsync(
            ExportJobContext exportJobContext,
            List<Long> articleIdsChunk,
            int threadNo
    ) {
        // Capture MDC from the caller thread (HTTP/job thread) so we can pass it to the async thread.
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();

        return CompletableFuture.supplyAsync(
                () -> {
                    // Restore MDC in the async thread.
                    if (parentMdc != null) MDC.setContextMap(parentMdc);
                    else MDC.clear();

                    // Add chunk-specific context for this async task.
                    MDC.put("chunkId", String.valueOf(threadNo));

                    try{
                        return articleChunkExportTransactionalService.exportChunkTransactional(exportJobContext, articleIdsChunk, threadNo);
                    } finally {
                        MDC.clear();
                    }
                },
                runnable -> exportJobThreadPool.execute(runnable)
        );
    }
}

