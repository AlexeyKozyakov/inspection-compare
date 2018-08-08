package com.inspectionDiff;
import com.intellij.openapi.progress.ProgressIndicator;
import com.sun.management.UnixOperatingSystemMXBean;
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
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public class XmlDiff {

    public static XmlDiffResult compareFolders(@NotNull String base, @NotNull String updated,
                                               @NotNull String outAdded, @NotNull String outRemoved, @Nullable String filter, @Nullable ProgressIndicator indicator) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        XmlDiffResult compareResult = new XmlDiffResult();
        Path leftFolder = Paths.get(base);
        Path rightFolder = Paths.get(updated);
        Path outputAdded = Paths.get(outAdded);
        if (Files.notExists(outputAdded)) {
            Files.createDirectories(outputAdded);
        }
        Path outputRemoved = Paths.get(outRemoved);
        if (Files.notExists(outputRemoved)) {
            Files.createDirectories(outputRemoved);
        }

        Map<String, Path> leftFiles = Files.list(leftFolder)
                .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                .collect(Collectors.toMap(f -> f.getFileName().toString(), f -> f));
        Map<String, Path> rightFiles = Files.list(rightFolder)
                .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                .collect(Collectors.toMap(f -> f.getFileName().toString(), f -> f));

        diffFiles(leftFiles, rightFiles, outputAdded, outputRemoved, compareResult, filter);
        diffContent(leftFiles, rightFiles, outputAdded, outputRemoved, filter, compareResult, indicator);
        Files.copy(leftFolder.resolve(".descriptions.xml"), outputAdded.resolve(".descriptions.xml"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(leftFolder.resolve(".descriptions.xml"), outputRemoved.resolve(".descriptions.xml"), StandardCopyOption.REPLACE_EXISTING);
        indicator.setFraction(1.0);
        return compareResult;
    }


    public static XmlDiffResult compareFiles(@NotNull Path base,@NotNull Path updated,
                               @NotNull Path outAdded, @NotNull Path outRemoved, @Nullable String filter) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        XmlDiffResult compareResult = new XmlDiffResult();
        Document left = read(base);
        Document right = read(updated);
        if(filter != null) {
             filterBoth(left, right, filter, compareResult);
        }
        Document added = diff(left, right, compareResult);
        Document removed = diff(right, left, null);
        write(outAdded, added);
        write(outRemoved, removed);
        return compareResult;
    }

    private static void diffContent(Map<String, Path> leftFiles, Map<String, Path> rightFiles, Path outputAdded, Path outputRemoved, String filter, XmlDiffResult compareResult, ProgressIndicator indicator ) throws ParserConfigurationException, TransformerException, SAXException, IOException {
        int progress = 0;
        for (Map.Entry<String, Path> file : leftFiles.entrySet()) {
            if (indicator.isCanceled()) {
                return;
            }
            if (rightFiles.containsKey(file.getKey())) {
                compareResult.add(compareFiles(file.getValue(), rightFiles.get(file.getKey()), outputAdded.resolve(file.getKey()),
                        outputRemoved.resolve(file.getKey()), filter));
                indicator.setFraction((double)progress / leftFiles.size());
            }
            ++progress;
        }
    }

    private static void diffFiles(Map<String, Path> leftFiles, Map<String, Path> rightFiles, Path outputAdded, Path outputRemoved, XmlDiffResult compareResult, String filter) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Map<String, Path> leftSansRight = new HashMap<>(leftFiles);
        leftSansRight.keySet().removeAll(rightFiles.keySet());
        Map<String, Path> rightSansLeft = new HashMap<>(rightFiles);
        rightSansLeft.keySet().removeAll(leftFiles.keySet());
        int problems;
        for (Map.Entry<String, Path> file : rightSansLeft.entrySet()) {
            Document doc = read(file.getValue());
            problems = filter(doc, filter)[1];
            compareResult.updatedProblems += problems;
            compareResult.added += problems;
            write(outputAdded.resolve(file.getKey()), doc);
        }
        for (Map.Entry<String, Path> file : leftSansRight.entrySet()) {
            Document doc = read(file.getValue());
            problems = filter(read(file.getValue()), filter)[1];
            compareResult.baseProblems += problems;
            compareResult.removed += problems;
            write(outputRemoved.resolve(file.getKey()), doc);
        }
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
        Map<List<String>, Element> rightModel = getModel(right);
        Map<List<String>, Element> leftSansRight = new HashMap<>(leftModel);
        leftSansRight.keySet().removeAll(rightModel.keySet());
        Map<List<String>, Element> rightSansLeft = new HashMap<>(rightModel);
        rightSansLeft.keySet().removeAll(leftModel.keySet());
        if (compareRes != null) {
            compareRes.baseProblems = leftModel.size();
            compareRes.updatedProblems = rightModel.size();
            compareRes.added =rightSansLeft.size();
            compareRes.removed = leftSansRight.size();
        }
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

    private static void write(Path output, Document document) throws TransformerException, IOException {
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(Files.newOutputStream(output));
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.MEDIA_TYPE, "text/xml");
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
        result.getOutputStream().close();
    }

    private static Document read(Path path) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputStream inputStream = Files.newInputStream(path);
        Document doc = builder.parse(inputStream);
        inputStream.close();
        return doc;
    }
}
