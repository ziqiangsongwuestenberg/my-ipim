package com.song.my_pim.service.exportjob.chunk;

import com.song.my_pim.service.exportjob.process.ExportJobContext;
import com.song.my_pim.service.exportjob.process.ExportJobResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import java.util.List;
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
        return CompletableFuture.supplyAsync(
                () -> articleChunkExportTransactionalService.exportChunkTransactional(exportJobContext, articleIdsChunk, threadNo),
                runnable -> exportJobThreadPool.execute(runnable)
        );
    }

}

