package com.song.my_pim.service.exportjob.writer.asyncWriter;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.dto.exportjob.ArticleExportDto;
import com.song.my_pim.dto.exportjob.ArticlePriceExportDto;
import com.song.my_pim.dto.exportjob.AttributeValueDto;
import com.song.my_pim.common.util.ExportNumberFormatter;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class XmlArticleWriter {

    public void writeArticle(XMLStreamWriter writer, ArticleExportDto article) throws XMLStreamException {
        writer.writeStartElement(ExportConstants.ARTICLE);
        writer.writeAttribute(ExportConstants.ID, article.getArticleId().toString());
        writer.writeAttribute("productType", article.getProductType());

        if (article.isProduct()) {
            writer.writeAttribute(ExportConstants.PRODUCT_NO, article.getProductNo());
        } else {
            writer.writeAttribute(ExportConstants.ARTICLE_NO, article.getArticleNo());
            writer.writeAttribute(ExportConstants.PRODUCT_NO, article.getProductNo());
        }

        writeAttributes(writer, article.getAttributes());

        if (article.isArticle() && article.getPrices() != null && !article.getPrices().isEmpty()) {
            writePrices(writer, new ArrayList<>(article.getPrices().values()));
        }

        writer.writeEndElement(); // article
    }

    private void writeAttributes(XMLStreamWriter writer, Map<String, AttributeValueDto> attrs) throws XMLStreamException {
        if (attrs == null) return;

        for (Map.Entry<String, AttributeValueDto> entry : attrs.entrySet()) {
            writer.writeStartElement(ExportConstants.ATTR);
            writer.writeAttribute(ExportConstants.ID, entry.getKey());

            AttributeValueDto dto = entry.getValue();
            if (dto != null && dto.getUnit() != null) {
                writer.writeAttribute(ExportConstants.UNIT, dto.getUnit());
            }
            if (dto != null && dto.getValue() != null) {
                writer.writeCharacters(dto.getValue());
            }

            writer.writeEndElement();
        }
    }

    private void writePrices(XMLStreamWriter writer, List<ArticlePriceExportDto> prices) throws XMLStreamException {
        writer.writeStartElement(ExportConstants.PRICES);

        for (ArticlePriceExportDto price : prices) {
            writer.writeStartElement(ExportConstants.PRICE);
            writer.writeAttribute(ExportConstants.TYPE, price.getPriceIdentifier());
            writer.writeAttribute(ExportConstants.CURRENCY, ExportConstants.EUR);
            writer.writeCharacters(ExportNumberFormatter.decimal2(price.getAmount()));
            writer.writeEndElement();
        }

        writer.writeEndElement(); // prices
    }
}
