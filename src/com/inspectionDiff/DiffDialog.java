package com.inspectionDiff;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.gui.DialogPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

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


    protected class RunAction extends DialogWrapperExitAction {

        public RunAction(String baseline, String updated, String filter, String addedWarnings, String removedWarnings) {
            super("Run", 0);

        }

        @Override
        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if (source instanceof JComponent) {
                JComponent src = (JComponent)source;
                DataContext dataContext = DataManager.getInstance().getDataContext(src);
                ProgressManager.getInstance().run(new Task.Backgroundable(DataKeys.PROJECT.getData(dataContext), "Tittle") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        for (int i = 0; i < 10; i++) {
                            if (indicator.isCanceled()) {
                                return;
                            }
                            indicator.setFraction(i  / 10.0);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
            close(0);
        }
    }
}
