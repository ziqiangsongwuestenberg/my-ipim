package com.song.my_pim.it;

import org.flywaydb.core.Flyway;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExportArticleControllerIT extends AbstractPostgresIT {

    @Autowired MockMvc mvc;
    @Autowired Flyway flyway;

    @MockBean
    private com.song.my_pim.service.exportjob.s3Service.ExportToS3Service exportToS3Service;

    @BeforeEach
    void resetDb() {
        flyway.clean();
        flyway.migrate();
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
    void export_articles_with_price_xml_to_s3_should_return_s3_uri_json() throws Exception {

        // mock: A fixed s3Uri is returned upon successful upload.
        org.mockito.Mockito.when(exportToS3Service.uploadXmlFile(
                org.mockito.ArgumentMatchers.any(java.nio.file.Path.class),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn("s3://my-ipim-exports/exports/articles/client-12/test.xml");

        mvc.perform(post("/api/exports/articlesWithAttributesAndPrice.xml/s3")
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
                        .value("s3://my-ipim-exports/exports/articles/client-12/test.xml"));
    }
}
