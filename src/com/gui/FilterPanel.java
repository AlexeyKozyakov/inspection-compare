package com.gui;

import com.inspectionDiff.OfflineViewer;
import com.inspectionDiff.XmlDiff;
import com.inspectionDiff.XmlDiffResult;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.intellij.openapi.ui.Messages.OK;


public class FilterPanel extends JBPanel implements DialogTab {
    private Project project;
    private JBLabel inspectionResultLabel = new JBLabel("Inspection result");
    private JBLabel filterLabel = new JBLabel("Filter");
    private TextFieldWithBrowseButton inspectionResult = new TextFieldWithBrowseButton();
    private LanguageTextField filter;
    private JBLabel outputLabel = new JBLabel("Output");
    private TextFieldWithBrowseButton output = new TextFieldWithBrowseButton();
    private boolean validationFlag = false;

    public FilterPanel(Project project) {
        this.project = project;
        filter = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        inspectionResult.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        output.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        inspectionResult.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                Path input = Paths.get(getInspectionResultAsStr());
                if (input != null && input.getParent() != null && input.getFileName() != null) {
                    output.setText(input.getParent().resolve(input.getFileName().toString() + "_filtered").toString());
                }
            }
        });
        loadState();
        add(inspectionResultLabel);
        add(inspectionResult);
        add(filterLabel);
        add(filter);
        add(outputLabel);
        add(output);
        VerticalLayout verticalLayout = new VerticalLayout(1);
        setLayout(verticalLayout);
        verticalLayout.addLayoutComponent(null, inspectionResultLabel);
        verticalLayout.addLayoutComponent(null, inspectionResult);
        verticalLayout.addLayoutComponent(null, filterLabel);
        verticalLayout.addLayoutComponent(null, filter);
        verticalLayout.addLayoutComponent(null, outputLabel);
        verticalLayout.addLayoutComponent(null, output);
    }

    @Override
    public JComponent getFocusComponent() {
        return inspectionResult;
    }

    @Override
    public ValidationInfo doValidate() {
        //don't show message about empty fields before button is pressed
        if (validationFlag) {
            if (getInspectionResultAsStr().isEmpty()) {
                return new ValidationInfo("Choose inspection results folder", inspectionResult.getTextField());
            }
            if (getOutputAsStr().isEmpty()) {
                return new ValidationInfo("Choose output folder", output.getTextField());
            }
        }
        if (!Files.exists(Paths.get(getInspectionResultAsStr()))) {
            return new ValidationInfo("Inspection results folder does not exist", inspectionResult.getTextField());
        }
        return null;
    }

    @Override
    public int run() {
        ValidationInfo validation = doValidate();
        boolean emptyInput = getInspectionResultAsStr().isEmpty() || getOutputAsStr().isEmpty();
        if (validation == null && !emptyInput) {
            //check if output folder exists and contains files
            Path outDir = Paths.get(getOutputAsStr());
            try {
                if (Files.exists(outDir) && Files.list(outDir).count() > 0) {
                    int message = Messages.showOkCancelDialog("Some files may be overwritten. Do you want to continue?", "The output directory already contains files", null);
                    if (message != OK) {
                        return CONTINUE;
                    }
                }
            } catch (AccessDeniedException e) {
                Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
            } catch (IOException e) {
                Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Filtering") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        XmlDiffResult result = XmlDiff.filterFolder(getInspectionResultAsStr(), getOutputAsStr(), getFilterAsStr(), indicator);
                        if (!indicator.isCanceled()) {
                            sendNotification(result);
                        }
                    } catch (AccessDeniedException e) {
                        Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
                    } catch (Exception e) {
                        Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
                    }
                }
            });
            saveState();
            return EXIT;
        } else {
            validationFlag = true;
            validation.component.grabFocus();
            return CONTINUE;
        }
    }

    public String getFilterAsStr() {
        return filter.getText();
    }

    public String getInspectionResultAsStr() {
        return inspectionResult.getText();
    }

    public String getOutputAsStr() {
        return output.getText();
    }

    private void sendNotification(XmlDiffResult result) {
        Notifications.Bus.notify(new Notification("Plugins notifications", null, "Completed!", null,
                "Initial warnings count: " + result.count + "<br>" +
                        "Filtered warnings count: " + result.filteredCount + "<br>" +
                        "<a href=\"filtered\">Open filtered</a>",
                NotificationType.INFORMATION, (notification, event) -> {
            if (event.getDescription().equals("filtered")) {
                VirtualFile filtered = LocalFileSystem.getInstance().refreshAndFindFileByPath(getOutputAsStr());
                OfflineViewer.openOfflineView(filtered, project);
            }
        }));
    }

    private void loadState() {
        inspectionResult.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.inspectionResult"));
        output.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.output"));
    }

    private void saveState() {
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.inspectionResult", inspectionResult.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.output", output.getText());
    }


}
