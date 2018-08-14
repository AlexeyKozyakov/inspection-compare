package com.inspection_diff;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;

public class DiffMenuItem extends AnAction {

    public DiffMenuItem() {
        super(" Filter/diff inspection results...");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            DiffDialog dialog = new DiffDialog(event.getProject());
            dialog.show();
        });
    }
}
