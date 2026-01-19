package com.song.my_pim.service.exportjob.writer;

import com.song.my_pim.common.exception.ExportWriteException;
import com.song.my_pim.dto.exportjob.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class XmlExportWithAttributesAndPricesWriter {
    public void write(List<ArticleExportDto> articles, OutputStream out)
            throws XMLStreamException {
        long time = System.currentTimeMillis();
        log.info("Start writing to the XML .");
        try {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(out, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("export");

            for (ArticleExportDto article : articles) {
                writeArticle(writer, article);
            }

            writer.writeEndElement(); // export
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            log.info("Writing to XML file complete : ms = {}",(System.currentTimeMillis() - time));
        } catch (XMLStreamException ex) {
            log.error("XML_WRITE failed after {} ms", (System.currentTimeMillis() - time), ex);
            throw new ExportWriteException("Failed to write XML export", ex);
        }
    }

    private void writeArticle(XMLStreamWriter writer,
                              ArticleExportDto article)
            throws XMLStreamException {

        writer.writeStartElement("article");
        writer.writeAttribute("id", article.getArticleId().toString());
        writer.writeAttribute("productType", article.getProductType());

        if (article.isProduct()) {
            writer.writeAttribute("productNo", article.getProductNo());
        } else {
            writer.writeAttribute("articleNo", article.getArticleNo());
            writer.writeAttribute("productNo", article.getProductNo());
        }


        writeAttributes(writer, article.getAttributes()); // id

        if (article.isArticle() && !article.getPrices().isEmpty()) {
            writePrices(writer, new ArrayList<>(article.getPrices().values())); //  todo, if this is a empty list, will report error ?
        }

        writer.writeEndElement(); // article
    }

    private void writeAttributes(XMLStreamWriter writer,
                                 Map<String, AttributeValueDto> attrs)
            throws XMLStreamException {

        for (Map.Entry<String, AttributeValueDto> entry : attrs.entrySet()) {
            writer.writeStartElement("attr");
            writer.writeAttribute("id", entry.getKey());

            if (entry.getValue().getUnit() != null) {
                writer.writeAttribute("unit", entry.getValue().getUnit());
            }

            writer.writeCharacters(entry.getValue().getValue());
            writer.writeEndElement();
        }
    }

    private void writePrices(XMLStreamWriter writer,
                             List<ArticlePriceExportDto> prices)
            throws XMLStreamException {

        writer.writeStartElement("prices");

        for (ArticlePriceExportDto price : prices) {
            writer.writeStartElement("price");
            writer.writeAttribute("type", price.getPriceIdentifier());
            writer.writeAttribute("currency", "EUR");
            writer.writeCharacters(price.getAmount().toPlainString());
            writer.writeEndElement();
        }

        writer.writeEndElement(); // prices
    }

}