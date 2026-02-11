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
        org.mockito.Mockito.verify(articleAsyncExportJobService).exportToXml(
                org.mockito.ArgumentMatchers.eq(12),
                org.mockito.ArgumentMatchers.any(com.song.my_pim.dto.exportjob.ArticleExportRequest.class),
                org.mockito.ArgumentMatchers.any(java.io.OutputStream.class)
        );
    }
}
