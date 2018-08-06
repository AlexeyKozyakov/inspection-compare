package com.gui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;
import java.awt.*;

public class DialogPanel extends JPanel {
    private JPanel container = new JPanel();
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
        container.add(baselineLabel);
        container.add(baseline);
        container.add(updatedLabel);
        container.add(updated);
        container.add(filterLabel);
        container.add(filter);
        container.add(addedWarningsLabel);
        container.add(addedWarnings);
        container.add(removedWarningsLabel);
        container.add(removedWarnings);
        container.setLayout(new GridLayout(10, 1));
        container.setPreferredSize(new Dimension(800, 600));
        add(container);
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

    public JPanel getContainer() {
        return container;
    }
}
