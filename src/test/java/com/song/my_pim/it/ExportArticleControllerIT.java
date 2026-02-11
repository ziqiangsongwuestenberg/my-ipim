package com.song.my_pim.it;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExportArticleControllerIT extends AbstractPostgresIT {

    @Autowired MockMvc mvc;
    @Autowired Flyway flyway;

    @MockitoBean
    private com.song.my_pim.service.exportjob.s3Service.ExportToS3Service exportToS3Service;

    @MockitoSpyBean
    private com.song.my_pim.service.exportjob.ArticleAsyncExportJobService articleAsyncExportJobService;

    @MockitoSpyBean
    private com.song.my_pim.service.exportjob.payload.ExportJobPayloadHandler payloadHandler;

    @MockitoSpyBean
    private com.song.my_pim.service.exportjob.chunk.ArticleChunkExportService articleChunkExportService;

    @MockitoSpyBean
    private com.song.my_pim.service.exportjob.chunk.ArticleChunkExportTransactionalService articleChunkExportTransactionalService;


    @DynamicPropertySource
    static void exportPayloadProps(DynamicPropertyRegistry registry) {
        String baseDir = Path.of(System.getProperty("java.io.tmpdir"), "my-pim-it-payload").toString();
        registry.add("mypim.export.payload.base-dir", () -> baseDir);
    }

    @BeforeEach
    void resetDb() {
        flyway.clean();
        flyway.migrate();
        org.mockito.Mockito.clearInvocations(articleAsyncExportJobService, exportToS3Service);
    }

    @Test
    void export_articles_xml_should_return_xml() throws Exception {
        MvcResult r = mvc.perform(post("/api/exports/articles.xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_XML)
                        .content("{\"client\":12,\"requestedBy\":\"it-test\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String ct = r.getResponse().getContentType();
        String body = r.getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(
                ct == null || ct.toLowerCase().contains("application/xml"),
                "Unexpected Content-Type: " + ct
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                body.contains("<export"),
                "Response body does not contain <export>:\n" + body
        );
    }


    @Test
    void export_articles_async_xml_to_s3_should_return_s3_uri_json() throws Exception {

        org.mockito.Mockito.when(exportToS3Service.uploadXmlFile(
                org.mockito.ArgumentMatchers.any(java.nio.file.Path.class),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn("s3://my-ipim-exports/exports/articles/client-12/async.xml");

        mvc.perform(post("/api/exports/articlesAsync.xml/s3")
                        .contentType("application/json")
                        .accept("application/json")
                        .content("""
                                {
                                  "client": 12,
                                  "requestedBy": "it-test",
                                  "includeDeleted": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.s3Uri")
                        .value("s3://my-ipim-exports/exports/articles/client-12/async.xml"));

        org.mockito.Mockito.verify(articleAsyncExportJobService).exportArticlesXmlToS3(
                org.mockito.ArgumentMatchers.eq(12),
                org.mockito.ArgumentMatchers.any(com.song.my_pim.dto.exportjob.ArticleExportRequest.class)
        );
        org.mockito.ArgumentCaptor<java.nio.file.Path> pathCaptor = org.mockito.ArgumentCaptor.forClass(java.nio.file.Path.class);
        org.mockito.ArgumentCaptor<String> keyCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(exportToS3Service).uploadXmlFile(pathCaptor.capture(), keyCaptor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                pathCaptor.getValue().getFileName().toString().startsWith("articles-export-"),
                "Unexpected temp file name: " + pathCaptor.getValue()
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                keyCaptor.getValue().contains("client-12/"),
                "Unexpected s3 key: " + keyCaptor.getValue()
        );
    }

    @Test
    void export_articles_async_xml_should_return_xml_and_use_async_service() throws Exception {
        MvcResult r = mvc.perform(post("/api/exports/articlesAsync.xml")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_XML)
                        .content("{\"client\":12,\"requestedBy\":\"it-test\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML)) // not allow null
                .andReturn();

        String body = r.getResponse().getContentAsString();

        // 1) XML 必须是“可解析”的（不是只 contains "<export"）
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        org.w3c.dom.Document doc = dbf.newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(body)));

        org.junit.jupiter.api.Assertions.assertEquals(
                "export",
                doc.getDocumentElement().getNodeName(),
                "Root element must be <export>, but was: " + doc.getDocumentElement().getNodeName()
        );

        // 2) 证明 controller 调用了 service.exportToXml(...)
        org.mockito.Mockito.verify(articleAsyncExportJobService).exportToXml(
                org.mockito.ArgumentMatchers.eq(12),
                org.mockito.ArgumentMatchers.any(com.song.my_pim.dto.exportjob.ArticleExportRequest.class),
                org.mockito.ArgumentMatchers.any(java.io.OutputStream.class)
        );

        // 3) 证明 “chunk async 导出真的发生了”（至少一个 chunk 被调度）
        // 如果你的 DB 里 client=12 没有文章，这个断言会失败（这是我们想要的：避免空跑也通过）
        org.mockito.Mockito.verify(articleChunkExportService, org.mockito.Mockito.timeout(3000).atLeastOnce())
                .exportChunkAsync(
                        org.mockito.ArgumentMatchers.any(com.song.my_pim.service.exportjob.process.ExportJobContext.class),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyInt()
                );

        // 4) 证明 chunk 的 transactional 真正跑过（更硬）
        org.mockito.Mockito.verify(articleChunkExportTransactionalService, org.mockito.Mockito.timeout(3000).atLeastOnce())
                .exportChunkTransactional(
                        org.mockito.ArgumentMatchers.any(com.song.my_pim.service.exportjob.process.ExportJobContext.class),
                        org.mockito.ArgumentMatchers.anyList(),
                        org.mockito.ArgumentMatchers.anyInt()
                );

        // 5) 抓到 ExportJobContext → 检查 payloadDir 下的文件确实被写出来（part + summary）
        org.mockito.ArgumentCaptor<com.song.my_pim.service.exportjob.process.ExportJobContext> ctxCaptor =
                org.mockito.ArgumentCaptor.forClass(com.song.my_pim.service.exportjob.process.ExportJobContext.class);

        org.mockito.Mockito.verify(payloadHandler, org.mockito.Mockito.atLeastOnce()).init(ctxCaptor.capture());

        com.song.my_pim.service.exportjob.process.ExportJobContext ctx = ctxCaptor.getValue();
        java.nio.file.Path payloadDir = ctx.getPayloadDir();

        java.nio.file.Path summary = payloadDir.resolve("logs").resolve("summary.json");
        org.junit.jupiter.api.Assertions.assertTrue(
                java.nio.file.Files.exists(summary),
                "summary.json not found: " + summary
        );

        // part-001.xml 至少要存在（证明写了 part 文件）
        java.nio.file.Path part1 = payloadDir.resolve("export/parts").resolve("part-001.xml");
        org.junit.jupiter.api.Assertions.assertTrue(
                java.nio.file.Files.exists(part1),
                "part file not found: " + part1
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                java.nio.file.Files.size(part1) > 0,
                "part file is empty: " + part1
        );
    }

}
