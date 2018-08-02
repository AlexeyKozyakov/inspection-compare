package com.inspectionDiff;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import gui.DialogPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DiffDialog extends DialogWrapper {
    private DialogPanel dialogPanel = new DialogPanel();
    private RunAction runAction = new RunAction();
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
        return new Action[] {getRunAction(), getCancelAction()};
    }

    public RunAction getRunAction() {
        return runAction;
    }
}
