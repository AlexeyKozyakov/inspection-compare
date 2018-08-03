package com.inspectionDiff;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class XmlDiff {

    public static XmlDiffResult compare(@NotNull String base,@NotNull String updated,
                               @NotNull String out, @Nullable String filter) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        XmlDiffResult compareResult = new XmlDiffResult();
        Document left = read(Paths.get(base));
        Document right = read(Paths.get(updated));
        Path output = Paths.get(out);
        if(filter != null) {
             filterBoth(left, right, filter, compareResult);
        }
        Document diff = diff(left, right, compareResult);
        write(output, diff);
        return compareResult;
    }

    private static void filterBoth(Document left, Document right, String substring, XmlDiffResult compareRes) {
        int [] leftBeforeAfter = filter(left, substring);
        compareRes.baseCount = leftBeforeAfter[0];
        compareRes.baseFiltered = leftBeforeAfter[1];
        int [] rightBeforeAfter = filter(right, substring);
        compareRes.updatedCount = rightBeforeAfter[0];
        compareRes.updatedFiltered = rightBeforeAfter[1];
    }

    private static int [] filter(Document document, String substring) {
        Element doc = document.getDocumentElement();
        NodeList nodes = document.getDocumentElement().getChildNodes();
        Map<List<String>, Element> result = new LinkedHashMap<>();
        int totalCount = 0, filteredCount = 0;
        for(int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node instanceof Element) {
                Element problem = (Element) node;
                Element description = (Element) problem.getElementsByTagName("description").item(0);
                if(description != null) {
                    totalCount++;
                    if(!description.getTextContent().contains(substring)) {
                        doc.removeChild(problem);
                    } else {
                        filteredCount++;
                    }
                }
            }
        }
        //System.out.println("Filtered: "+totalCount+" -> "+filteredCount);
        return new int [] {totalCount, filteredCount};
    }

    private static Map<List<String>, Element> getModel(Document document) {
        NodeList nodes = document.getDocumentElement().getChildNodes();
        Map<List<String>, Element> result = new LinkedHashMap<>();
        for(int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if(node instanceof Element) {
                Element problem = (Element) node;
                Element file = (Element) problem.getElementsByTagName("file").item(0);
                if(!file.getTextContent().endsWith(".java")) continue;
                Element line = (Element) problem.getElementsByTagName("line").item(0);
                Element description = (Element) problem.getElementsByTagName("description").item(0);
                if(file != null && line != null && description != null) {
                    result.put(Arrays.asList(file.getTextContent(), line.getTextContent(), description.getTextContent().replaceFirst("replaced with.+", "")), problem);
                }
            }
        }
        return result;
    }

    private static Document diff(Document left, Document right, XmlDiffResult compareRes) throws ParserConfigurationException {
        Map<List<String>, Element> leftModel = getModel(left);
        compareRes.baseProblems = leftModel.size();
        //System.out.println("Left problems: "+leftModel.size());
        Map<List<String>, Element> rightModel = getModel(right);
        compareRes.updatedProblems = rightModel.size();
        //System.out.println("Right problems: "+rightModel.size());
        Map<List<String>, Element> leftSansRight = new HashMap<>(leftModel);
        leftSansRight.keySet().removeAll(rightModel.keySet());
        Map<List<String>, Element> rightSansLeft = new HashMap<>(rightModel);
        rightSansLeft.keySet().removeAll(leftModel.keySet());
        compareRes.added =rightSansLeft.size();
        //System.out.println("Added: "+rightSansLeft.size());
        compareRes.removed = leftSansRight.size();
        //System.out.println("Removed: "+leftSansRight.size());
        Document res = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = res.createElement("problems");
        for (Element newChild : rightSansLeft.values()) {
            Element clone = (Element) newChild.cloneNode(true);
            res.adoptNode(clone);
            root.appendChild(clone);
        }
        res.appendChild(root);
        return res;
    }

    private static void write(Path output, Document document) throws ParserConfigurationException, TransformerException, IOException {
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(Files.newOutputStream(output));
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
    }

    private static Document read(Path path) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(Files.newInputStream(path));
    }
}
