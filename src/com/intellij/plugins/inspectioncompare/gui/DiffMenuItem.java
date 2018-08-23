package com.intellij.plugins.inspectioncompare.gui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;

public class DiffMenuItem extends AnAction implements DumbAware {

    public DiffMenuItem() {
        super(" Filter/Diff inspection results...");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            DiffDialog dialog = new DiffDialog(event.getProject());
            dialog.show();
        });
    }
}
