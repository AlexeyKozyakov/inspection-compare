package com.intellij.plugins.inspectioncompare.diff;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.intellij.openapi.progress.ProgressIndicator;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class XmlDiff {

    private static final LoadingCache<String, Map<List<String>, Element>> myCache = CacheBuilder.newBuilder().maximumSize(10).expireAfterAccess(20, TimeUnit.SECONDS).build(new CacheLoader<String, Map<List<String>, Element>>() {

        @Override
        public Map<List<String>, Element> load(@Nullable  String filename) throws Exception {
            return getModel(filename);
        }
    });

    public static XmlDiffResult compareFolders(@NotNull Path base, @NotNull Path updated,
                                               @Nullable Path outAdded, @Nullable Path outRemoved, @NotNull String filter,
                                               @NotNull String replaceFrom, @NotNull String replaceTo, @Nullable ProgressIndicator indicator) throws IOException, TransformerException, ParserConfigurationException, ExecutionException {
        XmlDiffResult compareResult = new XmlDiffResult();
        if (outAdded != null && outRemoved != null) {
            if (Files.notExists(outAdded)) {
                Files.createDirectories(outAdded);
            }
            if (Files.notExists(outRemoved)) {
                Files.createDirectories(outRemoved);
            }
        }
        Predicate<String> isAcceptableFileName = name -> name.toLowerCase().endsWith(".xml") && !name.equals(".descriptions.xml");
        Map<String, Path> leftFiles = Files.list(base)
                .filter(p -> isAcceptableFileName.test(p.getFileName().toString()))
                .collect(Collectors.toMap(f -> f.getFileName().toString(), f -> f));
        Map<String, Path> rightFiles = Files.list(updated)
                .filter(p -> isAcceptableFileName.test(p.getFileName().toString()))
                .collect(Collectors.toMap(f -> f.getFileName().toString(), f -> f));

        diffFiles(leftFiles, rightFiles, outAdded, outRemoved, compareResult, filter);
        diffContent(leftFiles, rightFiles, outAdded, outRemoved, filter, replaceFrom, replaceTo, compareResult, indicator);
        if (outAdded != null && outRemoved != null) {
            Files.copy(base.resolve(".descriptions.xml"), outAdded.resolve(".descriptions.xml"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(base.resolve(".descriptions.xml"), outRemoved.resolve(".descriptions.xml"), StandardCopyOption.REPLACE_EXISTING);
        }
        if (indicator != null)
            indicator.setFraction(1.0);
        return compareResult;
    }

    public static int getWarningsCount(Path file) throws ExecutionException {
        return filter(getModelFromCache(file), "")[0];
    }

    private static XmlDiffResult compareFiles(@NotNull Path base,@NotNull Path updated,
                               @Nullable Path outAdded, @Nullable Path outRemoved, @Nullable String filter,
                                             @NotNull String replaceFrom, @NotNull String replaceTo) throws IOException, TransformerException, ParserConfigurationException, ExecutionException {
        XmlDiffResult compareResult = new XmlDiffResult();
        Map<List<String>, Element> leftModel = getModelFromCache(base);
        Map<List<String>, Element> rightModel = getModelFromCache(updated);
        if(filter != null) {
             filterBoth(leftModel, rightModel, filter);
        }
        Document added = diff(leftModel, rightModel, replaceFrom, replaceTo, compareResult);
        Document removed = diff(rightModel, leftModel, replaceFrom, replaceTo, null);
        if (added != null && outAdded != null)
            write(outAdded, added);
        if (removed != null && outRemoved != null)
            write(outRemoved, removed);
        return compareResult;
    }

    public static XmlDiffResult filterFolder(@NotNull Path inspFolder, @Nullable Path outputFolder, String substring, ProgressIndicator indicator) throws IOException, ParserConfigurationException, TransformerException, ExecutionException {
        XmlDiffResult res = new XmlDiffResult();
        if (outputFolder != null && Files.notExists(outputFolder)) {
            Files.createDirectories(outputFolder);
        }
        Predicate<String> isAcceptableFileName = name -> name.toLowerCase().endsWith(".xml") && !name.equals(".descriptions.xml");
        List<Path> files = Files.list(inspFolder)
                .filter(p -> isAcceptableFileName.test(p.getFileName().toString()))
                .collect(Collectors.toList());
        int progress = 0;
        for (Path file : files) {
            if (indicator.isCanceled()) {
                return res;
            }
            Map<List<String>, Element> model = getModelFromCache(file);
            int [] beforeAfter = filter(model, substring);
            res.count += beforeAfter[0];
            res.filteredCount += beforeAfter[1];
            if (outputFolder != null) {
                if (model.size() > 0) {
                    write(outputFolder.resolve(file.getFileName().toString()), buildDocument(model));
                }
            }
            indicator.setFraction((double)progress / files.size());
            ++progress;
        }
        if (outputFolder != null) {
            Files.copy(inspFolder.resolve(".descriptions.xml"), outputFolder.resolve(".descriptions.xml"), StandardCopyOption.REPLACE_EXISTING);
        }
        indicator.setFraction(1.0);
        return res;
    }

    //compare files with the same names
    private static void diffContent(Map<String, Path> leftFiles, Map<String, Path> rightFiles, Path outputAdded, Path outputRemoved, String filter,
                                    String replaceFrom, String replaceTo, XmlDiffResult compareResult, ProgressIndicator indicator ) throws ParserConfigurationException, TransformerException, IOException, ExecutionException {
        int progress = 0;
        for (Map.Entry<String, Path> file : leftFiles.entrySet()) {
            if (indicator != null && indicator.isCanceled()) {
                return;
            }
            if (rightFiles.containsKey(file.getKey())) {
                compareResult.add(compareFiles(file.getValue(), rightFiles.get(file.getKey()), (outputAdded == null) ? null : outputAdded.resolve(file.getKey()),
                        (outputRemoved == null) ? null : outputRemoved.resolve(file.getKey()), filter, replaceFrom, replaceTo));
                if (indicator != null)
                    indicator.setFraction((double)progress / leftFiles.size());
            }
            ++progress;
        }
    }

    //check which files are added or removed
    private static void diffFiles(Map<String, Path> leftFiles, Map<String, Path> rightFiles, Path outputAdded, Path outputRemoved, XmlDiffResult compareResult, String filter) throws ParserConfigurationException, IOException, TransformerException, ExecutionException {
        Map<String, Path> leftSansRight = new HashMap<>(leftFiles);
        leftSansRight.keySet().removeAll(rightFiles.keySet());
        Map<String, Path> rightSansLeft = new HashMap<>(rightFiles);
        rightSansLeft.keySet().removeAll(leftFiles.keySet());
        int problems;
        for (Map.Entry<String, Path> file : rightSansLeft.entrySet()) {
            Map<List<String>, Element> model = getModelFromCache(file.getValue());
            problems = filter(model, filter)[1];
            compareResult.updatedProblems += problems;
            compareResult.added += problems;
            if (outputAdded != null) {
                write(outputAdded.resolve(file.getKey()), buildDocument(model));
            }
        }
        for (Map.Entry<String, Path> file : leftSansRight.entrySet()) {
            Map<List<String>, Element> model = getModelFromCache(file.getValue());
            problems = filter(model, filter)[1];
            compareResult.baseProblems += problems;
            compareResult.removed += problems;
            if (outputRemoved != null) {
                write(outputRemoved.resolve(file.getKey()), buildDocument(model));
            }
        }
    }

    private static void filterBoth(Map<List<String>, Element> leftModel, Map<List<String>, Element> rightModel, String substring) {
        filter(leftModel, substring);
        filter(rightModel, substring);
    }

    private static int [] filter(Map<List<String>, Element> model, String substring) {
        int before = model.size();
        model.entrySet().removeIf(entry -> {
            Element description = (Element) entry.getValue().getElementsByTagName("description").item(0);
            if (description != null) {
                return !Pattern.compile(substring).matcher(description.getTextContent()).find();
            }
            return false;
        });
        int after = model.size();
        return new int [] {before, after};
    }

    private static Map<List<String>, Element> getModelFromCache(Path path) throws ExecutionException {
        return new HashMap<>(myCache.get(path.toAbsolutePath().toString()));
    }

    private static Map<List<String>, Element> getModel(String filename) throws ParserConfigurationException, SAXException, IOException {
        Document document = read(Paths.get(filename));
        NodeList nodes = document.getDocumentElement().getChildNodes();
        Map<List<String>, Element> result = new LinkedHashMap<>();
        Map<List<String>, Integer> duplicateKeys = new HashMap<>();
        for(int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element problem = (Element) node;
                Element file = (Element) problem.getElementsByTagName("file").item(0);
                Element pack = (Element) problem.getElementsByTagName("package").item(0);
                Element line = (Element) problem.getElementsByTagName("line").item(0);
                Element description = (Element) problem.getElementsByTagName("description").item(0);
                if (description != null) {
                    List<String> key = Arrays.asList((file == null) ? "" : file.getTextContent(), (pack == null) ? "" : pack.getTextContent(), (line == null) ? "" : line.getTextContent(),
                            description.getTextContent().replaceFirst("replaced with.+", ""), String.valueOf(0));
                    if (result.containsKey(key)) {
                        List<String> duplicate = key.subList(0, 4);
                        Integer index = duplicateKeys.get(duplicate);
                        index = (index != null) ? index + 1 : 1;
                        duplicateKeys.put(duplicate, index);
                        key.set(4, String.valueOf(index));
                    }
                    result.put(key, problem);
                }
            }
        }
        return result;
    }

    private static Document diff(Map<List<String>, Element> leftModel, Map<List<String>, Element> rightModel, String replaceFrom, String replaceTo, XmlDiffResult compareRes) throws ParserConfigurationException {
        if (compareRes != null) {
            compareRes.baseProblems = leftModel.size();
            compareRes.updatedProblems = rightModel.size();
        }
        Map<List<String>, Element> leftNormalized = new HashMap<>();
        leftModel.forEach((key, value) -> {
            List<String> newKey = new ArrayList<>(key);
            newKey.set(3, newKey.get(3).replaceAll(replaceFrom, replaceTo));
            leftNormalized.put(newKey, value);
        });
        Map<List<String>, Element> rightNormalized = new HashMap<>();
        rightModel.forEach((key, value) -> {
            List<String> newKey = new ArrayList<>(key);
            newKey.set(3, newKey.get(3).replaceAll(replaceFrom, replaceTo));
            rightNormalized.put(newKey, value);
        });
        Map<List<String>, Element> leftSansRight = new HashMap<>(leftNormalized);
        leftSansRight.keySet().removeAll(rightNormalized.keySet());
        Map<List<String>, Element> rightSansLeft = new HashMap<>(rightNormalized);
        rightSansLeft.keySet().removeAll(leftNormalized.keySet());
        if (compareRes != null) {
            compareRes.added = rightSansLeft.size();
            compareRes.removed = leftSansRight.size();
        }
        if (rightSansLeft.isEmpty()) {
            return null;
        }
        return buildDocument(rightSansLeft);
    }

    private static Document buildDocument(Map<List<String>, Element> model) throws ParserConfigurationException {
        Document res = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = res.createElement("problems");
        for (Element newChild : model.values()) {
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
