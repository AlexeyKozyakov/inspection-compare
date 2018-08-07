package com.inspectionDiff;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class DiffMenuItem extends AnAction {

    public DiffMenuItem() {
        super(" Filter/diff inspection result");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        DiffDialog dialog = new DiffDialog(event.getProject(), true);
        dialog.show();
    }
}
