package com.intellij.plugins.inspectioncompare.util;

import com.intellij.analysis.PerformAnalysisInBackgroundOption;
import com.intellij.codeInspection.InspectionApplication;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.actions.ViewOfflineResultsAction;
import com.intellij.codeInspection.offline.OfflineProblemDescriptor;
import com.intellij.codeInspection.offlineViewer.OfflineViewParseUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class OfflineViewer {
    public static void openOfflineView(VirtualFile virtualFile, Project project) {
        if (virtualFile == null || !virtualFile.isDirectory()) return;

        final Map<String, Map<String, Set<OfflineProblemDescriptor>>> resMap =
                new HashMap<>();
        final String [] profileName = new String[1];
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
                InspectionsBundle.message("parsing.inspections.dump.progress.title"),
                true,
                new PerformAnalysisInBackgroundOption(project)) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                //for non project directories ensure refreshed directory
                VfsUtil.markDirtyAndRefresh(false, true, true, virtualFile);
                final VirtualFile[] files = virtualFile.getChildren();
                try {
                    for (final VirtualFile inspectionFile : files) {
                        if (inspectionFile.isDirectory()) continue;
                        final String shortName = inspectionFile.getNameWithoutExtension();
                        final String extension = inspectionFile.getExtension();
                        File ioFile = VfsUtilCore.virtualToIoFile(inspectionFile);
                        if (shortName.equals(InspectionApplication.DESCRIPTIONS)) {
                            profileName[0] = ReadAction.compute(() -> OfflineViewParseUtil.parseProfileName(ioFile));
                        }
                        else if ("xml".equals(extension)) {
                            resMap.put(shortName, ReadAction.compute(() -> OfflineViewParseUtil.parse(ioFile)));
                        }
                    }
                }
                catch (final Exception e) {  //all parse exceptions
                    ApplicationManager.getApplication()
                            .invokeLater(() -> Messages.showInfoMessage(e.getMessage(), InspectionsBundle.message("offline.view.parse.exception.title")));
                    throw new ProcessCanceledException(); //cancel process
                }
            }

            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    final String name = profileName[0];
                    ViewOfflineResultsAction.showOfflineView(project, name, resMap, InspectionsBundle.message("offline.view.title") +
                            " (" + (name != null ? name : InspectionsBundle.message("offline.view.editor.settings.title")) + ")");
                });
            }
        });
    }
}
