package com.song.my_pim.web.exportArticle;

import com.song.my_pim.common.exception.ExportJobInitException;
import com.song.my_pim.common.exception.ExportWriteException;
import com.song.my_pim.config.ExportJobProperties;
import com.song.my_pim.dto.exportjob.ArticleExportRequest;
import com.song.my_pim.dto.exportjob.response.ExportToS3Response;
import com.song.my_pim.service.exportjob.ArticleAsyncExportJobService;
import com.song.my_pim.service.exportjob.ArticleWithAttributesExportJobService;
import com.song.my_pim.service.exportjob.ArticleWithAttributesAndPricesExportJobService;
import com.song.my_pim.service.exportjob.XmlExportJob;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    private final ArticleAsyncExportJobService articleAsyncExportJobService;


    @PostMapping(
            value = "/articles.xml",
            consumes = "application/json",
            produces = "application/xml")
    public void exportArticlesXml(@RequestBody ArticleExportRequest request, HttpServletResponse response) throws IOException {
        exportInternal(request, response, articleWithAttributesExportJobService);
    }

    @PostMapping(
            value = "/articlesWithAttributesAndPrice.xml",
            consumes = "application/json",
            produces = "application/xml")
    public void exportArticlesWithPriceXml(@RequestBody ArticleExportRequest request, HttpServletResponse response) throws IOException {
        exportInternal(request, response, articleWithAttributesAndPricesExportJobService);
    }

    @PostMapping(
            value = "/articlesWithAttributesAndPrice.xml/s3",
            consumes = "application/json",
            produces = "application/json")
    public ExportToS3Response exportArticlesWithPriceXmlToS3(@RequestBody ArticleExportRequest request) {
        Integer client = requireClient(request);

        String s3Uri = articleWithAttributesAndPricesExportJobService.exportArticlesXmlToS3(client, request);
        return new ExportToS3Response(s3Uri);
    }

    @PostMapping(
            value = "/articlesAsync.xml",
            consumes = "application/json",
            produces = "application/xml")
    public void exportArticleAsyncXml(@RequestBody ArticleExportRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        exportInternal(request, response, articleAsyncExportJobService);
    }

    @PostMapping(
            value = "/articlesAsync.xml/s3",
            consumes = "application/json",
            produces = "application/json")
    public ExportToS3Response exportArticleAsyncXmlToS3(@RequestBody ArticleExportRequest request) {
        Integer client = requireClient(request);

        String s3Uri = articleAsyncExportJobService.exportArticlesXmlToS3(client, request);
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
        } catch (ExportJobInitException ex) {
            log.error("Export init failed. client={}, fileName={}", client, fileName, ex);
            writePlainErrorIfPossible(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to initialize export job.");

        } catch (ExportWriteException ex) {
            log.error("Export XML failed. client={}, fileName={}", client, fileName, ex);
            writePlainErrorIfPossible(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to write XML export.");

        } catch (Exception ex) {
            log.error("Export failed unexpectedly. client={}, fileName={}", client, fileName, ex);
            writePlainErrorIfPossible(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Export failed unexpectedly.");
        }
    }

    private void writePlainErrorIfPossible(HttpServletResponse response, int status, String message) throws IOException {
        if (!response.isCommitted()) {
            response.resetBuffer();
            response.setStatus(status);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write(message);
            response.flushBuffer();
        } else {
            log.warn("Response already committed, cannot write error response to client.");
        }
    }

    private Integer requireClient(ArticleExportRequest request) {
        Integer client = request.getClient();
        if (client == null) throw new IllegalArgumentException("client must be provided");
        return client;
    }

}
