package com.intellij.plugins.inspectioncompare.gui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ObjectUtils;

import java.awt.*;

class DialogTabs extends JBTabbedPane implements Disposable {

    public DialogTabs(Project project) {
        super(2);
        FilterDiffPanel filterDiffPanel = new FilterDiffPanel(project);
        FilterPanel filterPanel = new FilterPanel(project);
        insertTab("Diff", null, filterDiffPanel, "Diff inspection results", 0);
        insertTab("Filter", null, filterPanel, "Filter inspection results", 1);
        Disposer.register(this, filterDiffPanel);
        Disposer.register(this, filterPanel);
    }

    public DialogTab getCurrentTab() {
        Component currentTab = getSelectedComponent();
        return ObjectUtils.tryCast(currentTab, DialogTab.class);
    }

    @Override
    public void dispose() {
    }
}
