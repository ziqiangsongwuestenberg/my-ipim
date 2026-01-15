package com.song.my_pim.it;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExportArticleControllerIT extends AbstractPostgresIT {

    @Autowired MockMvc mvc;
    @Autowired Flyway flyway;

    @BeforeEach
    void resetDb() {
        flyway.clean();
        flyway.migrate();
    }

    @Test
    void export_articles_xml_should_return_xml() throws Exception {
        mvc.perform(post("/api/exports/articles.xml")
                .contentType("application/json")
                .header("Accept", "application/xml")
                .content("{\"client\":12,\"requestedBy\":\"it-test\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<export")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<article")));
    }
}
