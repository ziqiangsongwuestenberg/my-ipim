package com.song.my_pim.service.exportjob;

import com.song.my_pim.dto.exportjob.ArticleExportDto;
import lombok.extern.slf4j.Slf4j;

import javax.xml.stream.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Slf4j
public class XmlExportWriter {

    public void write(OutputStream os, Integer client, Iterable<ArticleExportDto> articles) {
        long time = System.currentTimeMillis();
        log.info("Start writing to the XML .");
        try {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            XMLStreamWriter w = factory.createXMLStreamWriter(os, StandardCharsets.UTF_8.name());

            w.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");

            w.writeStartElement("export");
            if (client != null) w.writeAttribute("client", String.valueOf(client));
            w.writeAttribute("generatedAt", OffsetDateTime.now().toString());

            for (ArticleExportDto a : articles) {
                w.writeStartElement("article");
                w.writeAttribute("id", String.valueOf(a.getArticleId()));
                if (a.getArticleNo() != null) w.writeAttribute("articleNo", a.getArticleNo());
                if (a.getProductNo() != null) w.writeAttribute("productNo", a.getProductNo());

                for (var e : a.getAttributes().entrySet()) {
                    w.writeStartElement("attr");
                    w.writeAttribute("id", e.getKey());

                    var av = e.getValue();
                    if (av.getUnit() != null && !av.getUnit().isBlank()) {
                        w.writeAttribute("unit", av.getUnit());
                    }

                    w.writeCharacters(av.getValue() == null ? "" : av.getValue());
                    w.writeEndElement();
                }

                w.writeEndElement(); // article
            }

            w.writeEndElement(); // export
            w.writeEndDocument();
            w.flush();
            w.close();
            log.info("Writing to XML file complete : ms = {}",(System.currentTimeMillis() - time));
        } catch (XMLStreamException ex) {
            log.error("XML_WRITE failed after {} ms", (System.currentTimeMillis() - time), ex);
            throw new RuntimeException("Failed to write XML export", ex);
        }
    }
}
