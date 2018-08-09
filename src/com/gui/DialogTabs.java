package com.gui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;

public class DialogTabs extends JBTabbedPane {
    private Project project;
    private FilterDiffPanel filterDiffPanel;
    private FilterPanel filterPanel;
    public DialogTabs(Project project) {
        super(2);
        this.project = project;
        filterDiffPanel = new FilterDiffPanel(project);
        filterPanel = new FilterPanel(project);
        insertTab("Filter and diff", null, filterDiffPanel, "Filter/diff inspection results", 0);
        insertTab("Filter only", null, filterPanel, "Filter inspection results", 1);
    }

    public FilterDiffPanel getFilterDiffPanel() {
        return filterDiffPanel;
    }

    public FilterPanel getFilterPanel() {
        return filterPanel;
    }
}
