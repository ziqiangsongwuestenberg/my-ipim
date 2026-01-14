package com.song.my_pim.web.exportArticle;

import com.song.my_pim.dto.export.ArticleExportRequest;
import com.song.my_pim.service.export.ArticleExportJobService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

        import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exports")
public class ExportArticleController {

    private final ArticleExportJobService job;

    @PostMapping(value = "/articles.xml",
            consumes = "application/json",
            produces = "application/xml")
    public void exportArticlesXml(@RequestBody ArticleExportRequest request, HttpServletResponse response) throws IOException {
        long time = System.currentTimeMillis();
        String fileName = "articles-export.xml";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        Integer client = request.getClient();
        if (client == null) {
            log.error("Client is missing in the request.");
            throw new IllegalArgumentException("client must be provided");
        }

        log.info("Export Article to the XML file request started: filename = {}, client = {}", fileName, client);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);

        try {
            job.exportToXml(client, request, response.getOutputStream());
            response.flushBuffer();
            log.info("Export Article to the XML file request finished in {} ms", System.currentTimeMillis() - time);
        } catch (Exception ex) {
            log.error("Export Article to the XML file request failed after {} ms", System.currentTimeMillis() - time, ex);
            throw ex;
        }
    }
}
