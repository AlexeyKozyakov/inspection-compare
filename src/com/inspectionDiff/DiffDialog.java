package com.inspectionDiff;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.gui.DialogPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DiffDialog extends DialogWrapper {

    private DialogPanel dialogPanel = new DialogPanel();
    protected DiffDialog(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
        init();
        setTitle("Test");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return dialogPanel;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        RunAction runAction = new RunAction(dialogPanel.getBaseAsStr(), dialogPanel.getUpdatedAsStr(),
                dialogPanel.getFilterAsStr(), dialogPanel.getAddedWarningsAsStr(), dialogPanel.getRemovedWarningsAsStr());
        return new Action[] { runAction, getCancelAction()};
    }

}
