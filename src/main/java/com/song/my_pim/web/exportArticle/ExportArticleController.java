package com.song.my_pim.web.exportArticle;

import com.song.my_pim.common.exception.ExportWriteException;
import com.song.my_pim.config.ExportJobProperties;
import com.song.my_pim.dto.exportjob.ArticleExportRequest;
import com.song.my_pim.dto.exportjob.response.ExportToS3Response;
import com.song.my_pim.service.exportjob.ArticleWithAttributesExportJobService;
import com.song.my_pim.service.exportjob.ArticleWithAttributesAndPricesExportJobService;
import com.song.my_pim.service.exportjob.XmlExportJob;
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
    private final ExportJobProperties props;
    private final ArticleWithAttributesExportJobService articleWithAttributesExportJobService;
    private final ArticleWithAttributesAndPricesExportJobService articleWithAttributesAndPricesExportJobService;

    @PostMapping(
            value = "/articles.xml",
            consumes = "application/json",
            produces = "application/xml"
    )
    public void exportArticlesXml(
            @RequestBody ArticleExportRequest request,
            HttpServletResponse response
    ) throws IOException {
        exportInternal(request, response, articleWithAttributesExportJobService);
    }

    @PostMapping(
            value = "/articlesWithAttributesAndPrice.xml",
            consumes = "application/json",
            produces = "application/xml"
    )
    public void exportArticlesWithPriceXml(
            @RequestBody ArticleExportRequest request,
            HttpServletResponse response
    ) throws IOException {
        exportInternal(request, response, articleWithAttributesAndPricesExportJobService);
    }

//    @PostMapping(
//            value = "/articles.xml/s3",
//            consumes = "application/json",
//            produces = "application/json"
//    )
//    public ExportToS3Response exportArticlesXmlToS3(@RequestBody ArticleExportRequest request) {
//        Integer client = requireClient(request);
//
//        String s3Uri = articleWithAttributesExportJobService.exportArticlesXmlToS3(client, request);
//        return new ExportToS3Response(s3Uri);
//    }

    @PostMapping(
            value = "/articlesWithAttributesAndPrice.xml/s3",
            consumes = "application/json",
            produces = "application/json"
    )
    public ExportToS3Response exportArticlesWithPriceXmlToS3(@RequestBody ArticleExportRequest request) {
        Integer client = requireClient(request);

        String s3Uri = articleWithAttributesAndPricesExportJobService.exportArticlesXmlToS3(client, request);
        return new ExportToS3Response(s3Uri);
    }


    private void exportInternal(
            ArticleExportRequest request,
            HttpServletResponse response,
            XmlExportJob job
    ) throws IOException {
        long time = System.currentTimeMillis();
        Integer client = requireClient(request);
        String fileName = props.getFileName();
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encoded);

        try {
            job.exportToXml(client, request, response.getOutputStream());
            response.flushBuffer();

            log.info("Export finished in {} ms", System.currentTimeMillis() - time);

        } catch (ExportWriteException ex) {
            log.error("Export XML failed", ex);
            if (!response.isCommitted()) {
                response.resetBuffer();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write("Failed to write XML export.");
                response.flushBuffer();
            }
        }
    }

    private Integer requireClient(ArticleExportRequest request) {
        Integer client = request.getClient();
        if (client == null) throw new IllegalArgumentException("client must be provided");
        return client;
    }

}
