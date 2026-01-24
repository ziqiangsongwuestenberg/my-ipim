package com.song.my_pim.service.exportjob.process;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public final class ExportJobUtil {

    private ExportJobUtil() {}

    public static ExportJobResult waitAllAndMerge(
            List<CompletableFuture<ExportJobResult>> futures,
            ExportJobResult merged
    ) {
        if (futures == null || futures.isEmpty()) return merged;

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<ExportJobResult> f : futures) {
            try {
                ExportJobResult sub = f.join();
                merged.merge(sub);
            } catch (Exception ex) {
                log.error("Chunk future failed", ex);
                merged.setFailed(merged.getFailed() + 1);
                merged.getErrors().add("Chunk future failed: " + ex.getMessage());
            }
        }
        return merged;
    }

    public static <T> List<List<T>> partition(List<T> list, int parts) {
        if (list == null || list.isEmpty()) return List.of();
        if (parts <= 1) return List.of(list);

        int size = list.size();
        int chunkSize = Math.max(1, (int) Math.ceil(size / (double) parts));

        java.util.ArrayList<List<T>> res = new java.util.ArrayList<>();
        for (int i = 0; i < size; i += chunkSize) {
            res.add(list.subList(i, Math.min(size, i + chunkSize)));
        }
        return res;
    }
}

