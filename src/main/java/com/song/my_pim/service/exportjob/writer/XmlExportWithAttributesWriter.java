package com.song.my_pim.service.exportjob.writer;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.common.exception.ExportWriteException;
import com.song.my_pim.dto.exportjob.ArticleExportDto;
import com.song.my_pim.dto.exportjob.AttributeValueDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
public class XmlExportWithAttributesWriter {
    private DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public void write(OutputStream os, Integer client, Iterable<ArticleExportDto> articles) {
        long time = System.currentTimeMillis();
        log.info("Start writing to the XML .");
        try {
            XMLStreamWriter xmlStreamWriter = getXmlStreamWriter(os);

            writeDocument(client, articles, xmlStreamWriter);

            log.info("Writing to XML file complete : ms = {}",(System.currentTimeMillis() - time));
        } catch (XMLStreamException ex) {
            log.error("XML_WRITE failed after {} ms", (System.currentTimeMillis() - time), ex);
            throw new ExportWriteException("Failed to write XML export", ex);
        }
    }

    private XMLStreamWriter getXmlStreamWriter(OutputStream os) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newFactory();
        XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(os, StandardCharsets.UTF_8.name());
        return xmlStreamWriter;}

    private void writeDocument(Integer client, Iterable<ArticleExportDto> articles, XMLStreamWriter xmlStreamWriter) throws XMLStreamException {
        xmlStreamWriter.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");

        xmlStreamWriter.writeStartElement(ExportConstants.EXPORT);
        if (client != null) xmlStreamWriter.writeAttribute(ExportConstants.CLIENT, String.valueOf(client));
        xmlStreamWriter.writeAttribute(ExportConstants.GENERATED_AT, OffsetDateTime.now().format(FMT));

        for (ArticleExportDto a : articles) {
            writeArticle(xmlStreamWriter, a);
        }

        xmlStreamWriter.writeEndElement(); // export
        xmlStreamWriter.writeEndDocument();
        xmlStreamWriter.flush();
        xmlStreamWriter.close();
    }

    private void writeArticle(XMLStreamWriter xmlStreamWriter, ArticleExportDto articleExportDto) throws XMLStreamException {
        xmlStreamWriter.writeStartElement(ExportConstants.ARTICLE);
        xmlStreamWriter.writeAttribute(ExportConstants.ID, String.valueOf(articleExportDto.getArticleId()));
        if (articleExportDto.getArticleNo() != null) xmlStreamWriter.writeAttribute(ExportConstants.ARTICLE_NO, articleExportDto.getArticleNo());
        if (articleExportDto.getProductNo() != null) xmlStreamWriter.writeAttribute(ExportConstants.PRODUCT_NO, articleExportDto.getProductNo());

        writeAttributes(xmlStreamWriter, articleExportDto.getAttributes());

        xmlStreamWriter.writeEndElement(); // article
    }

    private static void writeAttributes(XMLStreamWriter xmlStreamWriter, Map<String, AttributeValueDto> attrs) throws XMLStreamException {
        if (attrs == null || attrs.isEmpty()) return;
        for(Map.Entry<String, AttributeValueDto> entry : attrs.entrySet()) {
            xmlStreamWriter.writeStartElement(ExportConstants.ATTR);
            xmlStreamWriter.writeAttribute(ExportConstants.ID, entry.getKey());

            var av = entry.getValue();
            if (av.getUnit() != null && !av.getUnit().isBlank()) {
                xmlStreamWriter.writeAttribute(ExportConstants.UNIT, av.getUnit());
            }

            xmlStreamWriter.writeCharacters(av.getValue() == null ? "" : av.getValue());
            xmlStreamWriter.writeEndElement();
        }
    }
}
