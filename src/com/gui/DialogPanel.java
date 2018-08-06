package com.gui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;
import java.awt.*;

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
    public DialogPanel() {
        baseline.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
        updated.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
        addedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
        removedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new FileChooserDescriptor(false, true,
                false, false, false, false)));
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
        setLayout(new GridLayout(10, 1));
        setPreferredSize(new Dimension(800, 600));
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

}
