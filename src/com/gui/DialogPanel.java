package com.gui;

import com.intellij.codeInspection.InspectionApplication;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.uiDesigner.core.Spacer;
import com.twelvemonkeys.image.BufferedImageIcon;
import org.intellij.lang.annotations.RegExp;
import org.intellij.lang.regexp.RegExpFileType;
import org.intellij.lang.regexp.RegExpSyntaxHighlighterFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DialogPanel extends JPanel {
    private Project project;
    private JLabel baselineLabel = new JLabel("Baseline inspection result");
    private JLabel updatedLabel = new JLabel("Updated inspection result");
    private JLabel filterLabel = new JLabel("Filter");
    private JLabel addedWarningsLabel = new JLabel("Added warnings output");
    private JLabel removedWarningsLabel = new JLabel("Removed warnings output");
    private TextFieldWithBrowseButton baseline = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton updated = new TextFieldWithBrowseButton();
    private EditorTextField filter;
    private TextFieldWithBrowseButton addedWarnings = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton removedWarnings = new TextFieldWithBrowseButton();
    private JButton swapButton = new JButton();
    private JPanel buttonContainer = new JPanel();
    private Path basePath;
    private Path updatedPath;
    public DialogPanel(Project project) {
        this.project = project;
        filter = new EditorTextField("", project, RegExpFileType.INSTANCE);
        filter.setBackground(new Color(69, 73, 74));
        Image iconImage = null;
        try {
            iconImage = ImageIO.read(getClass().getResource("resources/swap1.png"));
        } catch (Exception e) {
            System.err.println("Cannot load button icon");
        }
        if (iconImage != null) {
            iconImage = iconImage.getScaledInstance(33, 43, Image.SCALE_DEFAULT);
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
    protected class InspectionChooseDescriptor extends FileChooserDescriptor {

        public InspectionChooseDescriptor() {
            super(false, true, false, false, false, false);
        }

        @Override
        public Icon getIcon(VirtualFile file) {
            if (file.isDirectory()) {
                if (file.findChild(InspectionApplication.DESCRIPTIONS + "." + StdFileTypes.XML.getDefaultExtension()) != null) {
                    return AllIcons.Nodes.InspectionResults;
                }
            }
            return super.getIcon(file);
        }
    }
    private void generateOutPaths() {
        addedWarnings.setText(basePath.getParent().resolve("from_" + basePath.getFileName() + "_to_" + updatedPath.getFileName()).toString());
        removedWarnings.setText(basePath.getParent().resolve("from_" + updatedPath.getFileName() + "_to_" + basePath.getFileName()).toString());
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

}
