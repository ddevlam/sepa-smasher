package nl.seiferd.sepa;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;


public class Smasher {

    private static final String START = "<Document><CstmrCdtTrfInitn>";
    private static final String END = "</CstmrCdtTrfInitn></Document>";

    public static void main(String[] args) throws IOException {
        new Smasher().smashFiles(args[0]);
    }

    private void smashFiles(String directoryPath) throws IOException {
        File directory = new File(directoryPath);

        if(!directory.isDirectory()){
            System.out.println("Unknown directoryPath " + directory.getAbsolutePath());
        } else {
            System.out.println("Going to smash files in " + directory.getAbsolutePath());

            File output = new File("output.xml");

            Files.write(output.toPath(), START.getBytes(Charset.defaultCharset()), StandardOpenOption.CREATE_NEW);
            writeDirectoryInputToOutput(directory, output);
            Files.write(output.toPath(), END.getBytes(Charset.defaultCharset()), StandardOpenOption.APPEND);
        }
    }

    private void writeDirectoryInputToOutput(File dir, File output) throws IOException {
        Files.list(dir.toPath())
                .filter(path -> path.toString().endsWith(".xml"))
                .peek(path -> System.out.println("Going to smash " + path.toString()))
                .map(path -> new File(path.toUri()))
                .forEach(file -> wrapAppendToXml(file, output));
    }

    private void wrapAppendToXml(File input, File output){
        try {
            appendToXml(input, output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendToXml(File input, File output) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(input);

        doc.getDocumentElement().normalize();
        Element document = (Element) doc.getFirstChild().getChildNodes();
        NodeList inf = document.getElementsByTagName("PmtInf");

        StringWriter stringWriter = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        for(int i = 0; i < inf.getLength(); i++) {
            transformer.transform(new DOMSource(inf.item(i)), new StreamResult(stringWriter));
        }

        Files.write(output.toPath(), stringWriter.toString().getBytes(Charset.defaultCharset()), StandardOpenOption.APPEND);
    }
}
