package com.song.my_pim.service.exportjob.writer.asyncWriter;

import com.song.my_pim.dto.exportjob.ArticleExportDto;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.List;

public class XmlExportPartWriter {

    private final XmlArticleWriter articleWriter = new XmlArticleWriter();

    public void writeArticlesFragment(List<ArticleExportDto> articles, OutputStream out) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(out, "UTF-8");

        for (ArticleExportDto article : articles) {
            articleWriter.writeArticle(writer, article);
        }

        writer.flush();
        writer.close();
    }
}
