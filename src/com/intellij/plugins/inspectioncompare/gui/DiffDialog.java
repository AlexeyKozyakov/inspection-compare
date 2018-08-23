package com.intellij.plugins.inspectioncompare.gui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

class DiffDialog extends DialogWrapper {
    private final DialogTabs dialogTabs;
    private final RunAction runAction = new RunAction();
    private final ClearAction clearAction = new ClearAction();
    DiffDialog(@Nullable Project project) {
        super(project, true);
        dialogTabs = new DialogTabs(project);
        init();
        setTitle("Filter/Diff Inspection Results");
        setModal(false);
        setValidationDelay(100);
        getRootPane().setDefaultButton(getButton(runAction));
        JButton clearButton = getButton(clearAction);
        if (clearButton != null) {
            clearButton.setMnemonic(KeyEvent.VK_C);
        }
        Disposer.register(getDisposable(), dialogTabs);
        JButton runButton = getButton(runAction);
        if (runButton != null) {
            dialogTabs.addChangeListener(e -> {
                if (dialogTabs.getCurrentTab() instanceof FilterDiffPanel) {
                    runButton.setText("Diff");
                } else {
                    runButton.setText("Filter");
                }
            });
        }
        startTrackingValidation();
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[] {clearAction};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return dialogTabs;
    }


    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[] { runAction, getCancelAction()};
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return dialogTabs.getCurrentTab().doValidate();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return dialogTabs.getCurrentTab().getFocusComponent();
    }

    class RunAction extends DialogWrapperExitAction {

        RunAction() {
            super("Diff", 0);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            int code = dialogTabs.getCurrentTab().run();
            if (code == DialogTab.EXIT) {
                close(CLOSE_EXIT_CODE);
            }
        }
    }

    class ClearAction extends AbstractAction {

        ClearAction() {
            super("Clear");
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            dialogTabs.getCurrentTab().clear();
        }
    }
}
