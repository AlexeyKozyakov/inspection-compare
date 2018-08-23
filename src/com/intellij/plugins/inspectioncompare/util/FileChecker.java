package com.intellij.plugins.inspectioncompare.util;

import com.intellij.plugins.inspectioncompare.diff.XmlDiff;
import com.intellij.ui.components.JBLabel;
import org.apache.commons.lang3.concurrent.ConcurrentException;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class FileChecker {
    public static void setInfo(JTextField file, Path path, JBLabel label) throws ExecutionException {
        if (file.getText().isEmpty()) {
            setInfo(null, label);
        } else {
            setInfo(getFolderInfo(path), label);
        }
    }

    private static void setInfo(String info, JBLabel label) {
        if (info != null) {
            label.setText(info);
            if (!label.isVisible()) {
                label.setVisible(true);
            }
        } else if (label.isVisible()) {
            label.setVisible(false);
        }
    }

    private static String getFolderInfo(Path folder) throws ExecutionException {
        String info = null;
        if (checkFile(folder)) {
            try {
                Predicate<String> isAcceptableFileName = name -> name.toLowerCase().endsWith(".xml") && !name.equals(".descriptions.xml");
                List<Path> xmlPaths = Files.list(folder).filter(p -> isAcceptableFileName.test(p.getFileName().toString())).collect(Collectors.toList());
                if(xmlPaths.size() == 1) {
                    info = "one .xml file with " + XmlDiff.getWarningsCount(xmlPaths.get(0)) + " warnings was found";
                } else {
                    info = xmlPaths.size() + " .xml files were found";
                }
            } catch (IOException e) {
                info = null;
            }
        }
        return info;
    }

    public static boolean checkFile(Path file) {
        boolean valid;
        try {
            valid = file != null && Files.exists(file) && Files.isDirectory(file) && Files.list(file).anyMatch(p -> p.getFileName().toString().equals(".descriptions.xml"));
        } catch (IOException e) {
            valid = false;
        }
        return valid;
    }

    public static boolean checkInfo(JLabel label) {
        return  (label.isVisible() && label.getText().startsWith("one"));
    }

    public static boolean checkRegexp(String regexp) {
        try {
            Pattern.compile(regexp);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     *
     * @param filename
     * @return file path if filename can be converted to path, otherwise null
     */
    public static Path getPathIfValid(String filename) {
        try {
            return Paths.get(filename);
        } catch (InvalidPathException e) {
            return null;
        }
    }
}
