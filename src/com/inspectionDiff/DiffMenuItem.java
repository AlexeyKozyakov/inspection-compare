package com.inspectionDiff;

import com.intellij.codeInspection.inferNullity.InferNullityAnnotationsAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;

public class DiffMenuItem extends AnAction {

    public DiffMenuItem() {
        super(" Filter/diff inspection result");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            DiffDialog dialog = new DiffDialog(event.getProject(), true);
            dialog.show();
        });
    }
}
