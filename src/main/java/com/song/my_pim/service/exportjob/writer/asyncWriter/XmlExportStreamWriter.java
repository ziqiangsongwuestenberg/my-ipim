package com.song.my_pim.service.exportjob.writer.asyncWriter;

import com.song.my_pim.common.constants.ExportConstants;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;

@Component
public class XmlExportStreamWriter {

    public XMLStreamWriter start(OutputStream out) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = factory.createXMLStreamWriter(out, "UTF-8");
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement(ExportConstants.EXPORT);
        return writer;
    }

    public void finish(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndElement(); // export
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }
}
