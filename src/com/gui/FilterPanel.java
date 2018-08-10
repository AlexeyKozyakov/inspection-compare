package com.gui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;


public class FilterPanel extends JBPanel implements DialogTab {
    private Project project;
    private JBLabel inspectionResultLabel = new JBLabel("Inspection result");
    private JBLabel filterLabel = new JBLabel("Filter");
    private TextFieldWithBrowseButton inspectionResult = new TextFieldWithBrowseButton();
    private LanguageTextField filter;
    private JBLabel outputLabel = new JBLabel("Output");
    private TextFieldWithBrowseButton output = new TextFieldWithBrowseButton();

    public FilterPanel(Project project) {
        this.project = project;
        filter = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        inspectionResult.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        output.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        add(inspectionResultLabel);
        add(inspectionResult);
        add(filterLabel);
        add(filter);
        add(outputLabel);
        add(output);
        VerticalLayout verticalLayout = new VerticalLayout(1);
        setLayout(verticalLayout);
        verticalLayout.addLayoutComponent(null, inspectionResultLabel);
        verticalLayout.addLayoutComponent(null, inspectionResult);
        verticalLayout.addLayoutComponent(null, filterLabel);
        verticalLayout.addLayoutComponent(null, filter);
        verticalLayout.addLayoutComponent(null, outputLabel);
        verticalLayout.addLayoutComponent(null, output);
    }

    @Override
    public JComponent getFocusComponent() {
        return inspectionResult;
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public int run() {
        return 0;
    }

    public String getFilterAsStr() {
        return filter.getText();
    }

    public String getInspectionResultAsStr() {
        return inspectionResult.getText();
    }


}
