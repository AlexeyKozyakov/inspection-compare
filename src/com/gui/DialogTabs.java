package com.gui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTabbedPane;

import java.awt.*;

public class DialogTabs extends JBTabbedPane {
    private FilterDiffPanel filterDiffPanel;
    private FilterPanel filterPanel;
    public DialogTabs(Project project) {
        super(2);
        filterDiffPanel = new FilterDiffPanel(project);
        filterPanel = new FilterPanel(project);
        insertTab("Filter and diff", null, filterDiffPanel, "Filter/diff inspection results", 0);
        insertTab("Filter only", null, filterPanel, "Filter inspection results", 1);
    }

    public DialogTab getCurrentTab() {
        Component currentTab = getSelectedComponent();
        if (currentTab instanceof DialogTab) {
            return (DialogTab) currentTab;
        } else {
            return null;
        }
    }
}
