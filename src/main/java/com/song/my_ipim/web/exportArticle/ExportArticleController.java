package com.song.my_ipim.web.exportArticle;

import com.song.my_ipim.service.export.ArticleExportJobService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

        import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/exports")
public class ExportArticleController {

    private final ArticleExportJobService job;

    @PostMapping(value = "/articles.xml", produces = "application/xml")
    public void exportArticlesXml(HttpServletResponse response) throws IOException {
        String fileName = "articles-export.xml";
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);

        job.exportToXml(response.getOutputStream());
    }
}
