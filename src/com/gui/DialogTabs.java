package com.gui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBTabbedPane;

import java.awt.*;

public class DialogTabs extends JBTabbedPane implements Disposable {
    private FilterDiffPanel filterDiffPanel;
    private FilterPanel filterPanel;
    public DialogTabs(Project project) {
        super(2);
        filterDiffPanel = new FilterDiffPanel(project);
        filterPanel = new FilterPanel(project);
        insertTab("Diff", null, filterDiffPanel, "Diff inspection results", 0);
        insertTab("Filter", null, filterPanel, "Filter inspection results", 1);
        Disposer.register(this, filterDiffPanel);
        Disposer.register(this, filterPanel);
    }

    public DialogTab getCurrentTab() {
        Component currentTab = getSelectedComponent();
        if (currentTab instanceof DialogTab) {
            return (DialogTab) currentTab;
        } else {
            return null;
        }
    }

    @Override
    public void dispose() {
    }
}
