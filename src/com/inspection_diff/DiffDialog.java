package com.inspection_diff;

import com.gui.DialogTabs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

import static com.gui.DialogTab.EXIT;

public class DiffDialog extends DialogWrapper {
    private final DialogTabs dialogTabs;
    protected DiffDialog(@Nullable Project project) {
        super(project, true);
        dialogTabs = new DialogTabs(project);
        init();
        setTitle("Filter/Diff Inspection Results");
        setModal(false);
        Disposer.register(getDisposable(), dialogTabs);
        setValidationDelay(100);
        startTrackingValidation();
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[] {new ClearAction()};
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return dialogTabs;
    }


    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[] { new RunAction(), getCancelAction()};
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

    protected class RunAction extends DialogWrapperExitAction {

        public RunAction() {
            super("Run", 0);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            int code = dialogTabs.getCurrentTab().run();
            if (code == EXIT) {
                close(CLOSE_EXIT_CODE);
            }
        }
    }

    protected class ClearAction extends AbstractAction {

        public ClearAction() {
            super("Clear");
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            dialogTabs.getCurrentTab().clear();
        }
    }
}
