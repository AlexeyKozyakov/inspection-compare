package com.inspectionDiff;

import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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
        RunAction runAction = new RunAction();
        return new Action[] { runAction, getCancelAction()};
    }

    protected class RunAction extends DialogWrapperExitAction {

        public RunAction() {
            super("Run", 0);
            setResizable(false);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            DataContext dataContext = DataManager.getInstance().getDataContext(dialogPanel);
            ProgressManager.getInstance().run(new Task.Backgroundable(DataKeys.PROJECT.getData(dataContext), "Comparing") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        XmlDiffResult result = XmlDiff.compare(dialogPanel.getBaseAsStr(), dialogPanel.getUpdatedAsStr(), dialogPanel.getAddedWarningsAsStr(), dialogPanel.getFilterAsStr());
                        indicator.setFraction(1.0);
                        Notifications.Bus.notify(new Notification("Plugins notifications", null, "Completed!", null,
                                "Baseline warnings count: " + result.baseProblems + "<br>" +
                                        "Updated warnings count: " + result.updatedProblems + "<br>" +
                                        "Added warnings: " + result.added + "<br>" +
                                        "Removed warnings: " + result.removed + "<br>",
                                NotificationType.INFORMATION, null));
                    } catch (Exception e) {

                    }
                }
            });
            close(0);
        }

    }
}
