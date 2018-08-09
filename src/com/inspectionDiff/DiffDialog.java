package com.inspectionDiff;

import com.gui.FilterDiffPanel;
import com.gui.DialogTabs;
import com.gui.FilterPanel;
import com.intellij.analysis.PerformAnalysisInBackgroundOption;
import com.intellij.codeInspection.InspectionApplication;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.actions.ViewOfflineResultsAction;
import com.intellij.codeInspection.offline.OfflineProblemDescriptor;
import com.intellij.codeInspection.offlineViewer.OfflineViewParseUtil;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.intellij.openapi.ui.Messages.OK;

public class DiffDialog extends DialogWrapper {
    private final DialogTabs dialogTabs;
    private final FilterDiffPanel filterDiffPanel;
    private final FilterPanel filterPanel;
    private String addedWarnings;
    private String removedWarnings;
    private Project project;
    private XmlDiffResult result;
    private boolean validationFlagFilterDiff = false;
    private boolean validationFlagFilterOnly = false;
    protected DiffDialog(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
        this.project = project;
        dialogTabs = new DialogTabs(project);
        filterDiffPanel = dialogTabs.getFilterDiffPanel();
        filterPanel = dialogTabs.getFilterPanel();
        init();
        setTitle("Filter/diff inspection Results");
        setModal(false);
        setValidationDelay(100);
        startTrackingValidation();
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
        if (dialogTabs.getSelectedIndex() == 0) {
            return validateFilterDiff();
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return filterDiffPanel.getBaseline();
    }

    private ValidationInfo validateFilterDiff() {
        //don't show message about empty fields before button is pressed
        if (validationFlagFilterDiff) {
            if (filterDiffPanel.getBaseAsStr().isEmpty()) {
                return new ValidationInfo("Choose baseline folder", filterDiffPanel.getBaseline().getTextField());
            }
            if (filterDiffPanel.getUpdatedAsStr().isEmpty()) {
                return new ValidationInfo("Choose updated folder", filterDiffPanel.getUpdated().getTextField());
            }
            if (filterDiffPanel.getAddedWarningsAsStr().isEmpty()) {
                return new ValidationInfo("Choose added warnings out folder", filterDiffPanel.getAddedWarnings().getTextField());
            }
            if (filterDiffPanel.getRemovedWarningsAsStr().isEmpty()) {
                return new ValidationInfo("Choose removed warnings out folder", filterDiffPanel.getRemovedWarnings().getTextField());
            }
        }
        if (!Files.exists(Paths.get(filterDiffPanel.getBaseAsStr())) ) {
            return new ValidationInfo("Baseline folder does not exist", filterDiffPanel.getBaseline().getTextField());
        }
        if (!Files.exists(Paths.get(filterDiffPanel.getUpdatedAsStr())) ) {
            return new ValidationInfo("Updated folder does not exist", filterDiffPanel.getUpdated().getTextField());
        }
        if (!filterDiffPanel.getBaseAsStr().isEmpty() && filterDiffPanel.getBaseAsStr().equals(filterDiffPanel.getUpdatedAsStr())) {
            return new ValidationInfo("Choose different baseline and updated folders", filterDiffPanel.getBaseline().getTextField());
        }
        if (!filterDiffPanel.getAddedWarningsAsStr().isEmpty() && filterDiffPanel.getAddedWarningsAsStr().equals(filterDiffPanel.getRemovedWarningsAsStr())) {
            return new ValidationInfo("Choose different output folders", filterDiffPanel.getAddedWarnings().getTextField());
        }
        return null;
    }

/*    private ValidationInfo validateFilterOnly() {
        if (validationFlagFilterOnly) {
            if (filterPanel.getInspectionResultAsStr().isEmpty()) {
                return new ValidationInfo("Choose inspection results folder");
            }
        }
    }*/

    protected class RunAction extends DialogWrapperExitAction {

        public RunAction() {
            super("Run", 0);
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            if (dialogTabs.getSelectedIndex() == 0) {
                doFilterDiff();
            } else {
                doFilterOnly();
            }
        }

        private void doFilterDiff() {
            ValidationInfo validation = doValidate();
            //check if input fields is empty
            boolean emptyInput = filterDiffPanel.getBaseAsStr().isEmpty() || filterDiffPanel.getUpdatedAsStr().isEmpty() || filterDiffPanel.getAddedWarningsAsStr().isEmpty() || filterDiffPanel.getRemovedWarningsAsStr().isEmpty();
            if (validation == null && !emptyInput) {
                //check if output folders exists and contain files
                Path addedDir = Paths.get(filterDiffPanel.getAddedWarningsAsStr());
                Path removedDir = Paths.get(filterDiffPanel.getAddedWarningsAsStr());
                try {
                    if (Files.exists(addedDir) && Files.list(addedDir).count() > 0 || Files.exists(removedDir) && Files.list(removedDir).count() > 0) {

                        int message = Messages.showOkCancelDialog("Some files may be overwritten. Do you want to continue?", "The output directory already contains files", null);
                        if (message != OK) {
                            return;
                        }
                    }
                } catch (IOException e) {
                    Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
                }
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Comparing") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            addedWarnings = filterDiffPanel.getAddedWarningsAsStr();
                            removedWarnings = filterDiffPanel.getRemovedWarningsAsStr();
                            result = XmlDiff.compareFolders(filterDiffPanel.getBaseAsStr(), filterDiffPanel.getUpdatedAsStr(), addedWarnings,
                                    removedWarnings, filterDiffPanel.getFilterAsStr(), indicator);
                            sendNotification(result, project);
                        } catch (AccessDeniedException e) {
                            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
                        } catch (Exception e) {
                            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
                        }
                    }
                });
                close(0);
            } else {
                validationFlagFilterDiff = true;
                validation.component.grabFocus();
            }
        }

        private void doFilterOnly() {
            //to do
        }

        //send notification with compare results
        private void sendNotification(XmlDiffResult result, Project project) {
            Notifications.Bus.notify(new Notification("Plugins notifications", null, "Completed!", null,
                    "Baseline warnings count: " + result.baseProblems + "<br>" +
                            "Updated warnings count: " + result.updatedProblems + "<br>" +
                            "Added warnings: " + result.added + "<br>" +
                            "Removed warnings: " + result.removed + "<br>" +
                            "<a href=\"added\">Open added</a>  " +
                            "<a href=\"removed\">Open removed</a>",
                    NotificationType.INFORMATION, (notification, event) -> {
                        if (event.getDescription().equals("added")) {
                            VirtualFile added = LocalFileSystem.getInstance().refreshAndFindFileByPath(addedWarnings);
                            openOfflineView(added, project);
                        }
                        if (event.getDescription().equals("removed")) {
                            VirtualFile removed = LocalFileSystem.getInstance().refreshAndFindFileByPath(removedWarnings);
                            openOfflineView(removed, project);
                        }
                    }));
        }

        private void openOfflineView(VirtualFile virtualFile, Project project) {
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
                            if (shortName.equals(InspectionApplication.DESCRIPTIONS)) {
                                profileName[0] = ReadAction.compute(() -> OfflineViewParseUtil.parseProfileName(LoadTextUtil.loadText(inspectionFile).toString()));
                            }
                            else if ("xml".equals(extension)) {
                                resMap.put(shortName, ReadAction.compute(() -> OfflineViewParseUtil.parse(LoadTextUtil.loadText(inspectionFile).toString())));
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
}
