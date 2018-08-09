package com.gui;

import com.intellij.codeInspection.InspectionApplication;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.regexp.RegExpFileType;
import org.intellij.lang.regexp.RegExpLanguage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilterDiffPanel extends JBPanel {
    private Project project;
    private JBLabel baselineLabel = new JBLabel("Baseline inspection result");
    private JBLabel updatedLabel = new JBLabel("Updated inspection result");
    private JBLabel filterLabel = new JBLabel("Filter");
    private JBLabel addedWarningsLabel = new JBLabel("Added warnings output");
    private JBLabel removedWarningsLabel = new JBLabel("Removed warnings output");
    private TextFieldWithBrowseButton baseline = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton updated = new TextFieldWithBrowseButton();
    private EditorTextField filter;
    private TextFieldWithBrowseButton addedWarnings = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton removedWarnings = new TextFieldWithBrowseButton();
    private JButton swapButton = new JButton();
    private JBPanel buttonContainer = new JBPanel();
    private Path basePath;
    private Path updatedPath;
    public FilterDiffPanel(Project project) {
        this.project = project;
        filter = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        filter.setBackground(baseline.getTextField().getBackground());
        Image iconImage = null;
        try {
            iconImage = ImageIO.read(getClass().getResource("resources/swap1.png"));
        } catch (Exception e) {
            System.err.println("Cannot load button icon");
        }
        if (iconImage != null) {
            iconImage = dye((BufferedImage) iconImage, JBColor.foreground()).getScaledInstance(30, 38, Image.SCALE_DEFAULT);
            swapButton.setIcon(new ImageIcon(iconImage));
        }
        swapButton.setPreferredSize(new Dimension(50, 50));
        swapButton.setToolTipText("Swap input folders");
        buttonContainer.setLayout(new BorderLayout(5, 5));
        buttonContainer.add(swapButton, BorderLayout.LINE_START);
        swapButton.addActionListener(actionEvent -> {
            String tmp;
            tmp = baseline.getText();
            baseline.setText(updated.getText());
            updated.setText(tmp);
            grabFocus();
        });

        baseline.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        updated.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        addedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        removedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        updated.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updatedPath = Paths.get(updated.getText());
                if (!baseline.getText().isEmpty()) {
                    basePath = Paths.get(baseline.getText());
                    generateOutPaths();
                }
            }
        });
        baseline.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                basePath = Paths.get(baseline.getText());
                if (!updated.getText().isEmpty()) {
                    updatedPath = Paths.get(updated.getText());
                    generateOutPaths();
                }
            }
        });
        setPreferredSize(new Dimension(800, 500));
        VerticalLayout layout = new VerticalLayout(1);
        add(baselineLabel);
        add(baseline);
        add(buttonContainer);
        add(updatedLabel);
        add(updated);
        add(filterLabel);
        add(filter);
        add(addedWarningsLabel);
        add(addedWarnings);
        add(removedWarningsLabel);
        add(removedWarnings);
        layout.addLayoutComponent(baselineLabel, null);
        layout.addLayoutComponent(baseline, null);
        layout.addLayoutComponent(buttonContainer, null);
        layout.addLayoutComponent(updatedLabel, null);
        layout.addLayoutComponent(updated, null);
        layout.addLayoutComponent(filterLabel, null);
        layout.addLayoutComponent(filter, null);
        layout.addLayoutComponent(addedWarningsLabel, null);
        layout.addLayoutComponent(addedWarnings, null);
        layout.addLayoutComponent(removedWarningsLabel, null);
        layout.addLayoutComponent(removedWarnings, null);
        setLayout(layout);
    }
    private void generateOutPaths() {
        String baseFilename = (basePath == null || basePath.getFileName() == null) ? "" : basePath.getFileName().toString();
        String updatedFilename = (updatedPath == null || updatedPath.getFileName() == null) ? "" : updatedPath.getFileName().toString();
        Path parent;
        if (basePath != null && basePath.getParent() != null) {
            parent = basePath.getParent();
        }
        else {
            parent = updatedPath.getParent();
        }
        if (parent != null) {
            addedWarnings.setText(parent.resolve("from_" + baseFilename + "_to_" + updatedFilename).toString());
            removedWarnings.setText(parent.resolve("from_" + updatedFilename + "_to_" + baseFilename).toString());
        }
    }
    public String getBaseAsStr() {
        return baseline.getText();
    }
    public String getUpdatedAsStr() {
        return updated.getText();
    }
    public String getFilterAsStr() {
        return filter.getText();
    }
    public String getAddedWarningsAsStr() {
        return addedWarnings.getText();
    }
    public String getRemovedWarningsAsStr() {
        return removedWarnings.getText();
    }

    public TextFieldWithBrowseButton getBaseline() {
        return baseline;
    }
    public TextFieldWithBrowseButton getUpdated() {
        return updated;
    }

    public TextFieldWithBrowseButton getAddedWarnings() {
        return addedWarnings;
    }

    public TextFieldWithBrowseButton getRemovedWarnings() {
        return removedWarnings;
    }

    private static BufferedImage dye(BufferedImage image, Color color)
    {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage dyed = UIUtil.createImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dyed.createGraphics();
        g.drawImage(image, 0,0, null);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(color);
        g.fillRect(0,0,w,h);
        g.dispose();
        return dyed;
    }

}
