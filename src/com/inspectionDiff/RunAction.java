package com.inspectionDiff;


import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RunAction extends AbstractAction {

    public RunAction(String baseline, String updated, String filter, String addedWarnings, String removedWarnings) {
        super("Run");

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
    }

}
