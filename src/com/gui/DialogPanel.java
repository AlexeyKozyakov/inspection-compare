package com.gui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.panels.VerticalLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DialogPanel extends JPanel {
    private JLabel baselineLabel = new JLabel("Baseline inspection result");
    private JLabel updatedLabel = new JLabel("Updated inspection result");
    private JLabel filterLabel = new JLabel("Filter");
    private JLabel addedWarningsLabel = new JLabel("Added warnings output");
    private JLabel removedWarningsLabel = new JLabel("Removed warnings output");
    private TextFieldWithBrowseButton baseline = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton updated = new TextFieldWithBrowseButton();
    private JTextField filter = new JTextField();
    private TextFieldWithBrowseButton addedWarnings = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton removedWarnings = new TextFieldWithBrowseButton();
    private Path basePath;
    private Path updatedPath;
    public DialogPanel() {
        baseline.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
        updated.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
        addedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
        removedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
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
