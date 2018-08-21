package com.gui;

import com.intellij.codeEditor.printing.ExportToHTMLSettings;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import com.util.FileChecker;
import com.util.OfflineViewer;
import com.inspection_diff.XmlDiff;
import com.inspection_diff.XmlDiffResult;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jdesktop.swingx.VerticalLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.intellij.openapi.ui.Messages.OK;


public class FilterPanel extends JBPanel implements DialogTab, Disposable {
    private Project project;
    private JBLabel inspectionResultLabel = new JBLabel("Inspection result");
    private JBLabel filterLabel = new JBLabel("Filter");
    private TextFieldWithBrowseButton inspectionResult = new TextFieldWithBrowseButton();
    private LanguageTextFieldWithHistory filter;
    private JBLabel outputLabel = new JBLabel("Output");
    private TextFieldWithBrowseButton output = new TextFieldWithBrowseButton();
    private JBLabel inputInfo = new JBLabel();
    private JBLabel resultsPreview = new JBLabel();
    private JButton lastInspection = new JButton();
    private boolean validationFlag = false;
    private Alarm previewAlarm = new Alarm(this);
    private JBPanel buttonContainer = new JBPanel(new BorderLayout());

    public FilterPanel(Project project) {
        this.project = project;
        lastInspection.setToolTipText("Open folder which contains latest exported results " + "(" + ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY + ")" );
        buttonContainer.add(inspectionResult);
        buttonContainer.add(lastInspection, BorderLayout.LINE_END);
        filter = new LanguageTextFieldWithHistory(10, "Inspection.Compare.Plugin.filterHistory", project, RegExpLanguage.INSTANCE, new JBPanel(new BorderLayout()));
        filter.addToWrapper();
        inspectionResult.addBrowseFolderListener(null, "Select directory which contains inspection results", project, new InspectionChooseDescriptor());
        output.addBrowseFolderListener(null, "Select output directory", project, new InspectionChooseDescriptor());
        inputInfo.setVisible(false);
        inputInfo.setFontColor(UIUtil.FontColor.BRIGHTER);
        resultsPreview.setVisible(false);
        resultsPreview.setFontColor(UIUtil.FontColor.BRIGHTER);
        inspectionResult.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!inspectionResult.getText().isEmpty()) {
                    Path input = Paths.get(getInspectionResultAsStr());
                    FileChecker.setInfo(inspectionResult.getTextField(), inputInfo);
                    if (input != null && input.getParent() != null && input.getFileName() != null) {
                        output.setText(input.getParent().resolve(input.getFileName().toString() + "_filtered").toString());
                    }
                    preview();
                }
                if (!ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY.isEmpty() && !ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY.equals(inspectionResult.getText())) {
                    if (!lastInspection.isVisible()) {
                        lastInspection.setVisible(true);
                    }
                } else {
                    if (lastInspection.isVisible()) {
                        lastInspection.setVisible(false);
                    }
                }
            }
        });
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
                preview();
            }
        });
        loadState();
        lastInspection.setVisible(false);
        if (!ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY.isEmpty() && !ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY.equals(inspectionResult.getText())) {
            lastInspection.setVisible(true);
        }
        lastInspection.setText("L");
        lastInspection.setPreferredSize(new Dimension(50, 50));
        lastInspection.addActionListener(e -> {
            inspectionResult.setText(ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY);
            lastInspection.setVisible(false);
        });
        add(inspectionResultLabel);
        add(buttonContainer);
        add(inputInfo);
        add(filterLabel);
        add(filter.getWrapper());
        add(outputLabel);
        add(output);
        add(resultsPreview);
        installLayout();
        Disposer.register(this, inspectionResult);
        Disposer.register(this, output);
        preview();
    }

    @Override
    public JComponent getFocusComponent() {
        return inspectionResult;
    }

    @Override
    public void clear() {
        inspectionResult.setText("");
        filter.setText("");
        output.setText("");
        getFocusComponent().grabFocus();
    }

    @Override
    public TextFieldWithBrowseButton getLastField() {
        return output;
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
            if (!FileChecker.checkRegexp(getFilterAsStr())) {
                return new ValidationInfo("Syntax error in regex", filter);
            }
        }
        if (!getInspectionResultAsStr().isEmpty() && Files.notExists(Paths.get(getInspectionResultAsStr()))) {
            return new ValidationInfo("Folder doesn't exist", inspectionResult.getTextField());
        }
        if (!getInspectionResultAsStr().isEmpty() && !FileChecker.checkFile(Paths.get(getInspectionResultAsStr()))) {
            return new ValidationInfo("Folder doesn't contain inspection results", inspectionResult.getTextField());
        }
        return null;
    }

    @Override
    public int run() {
        ValidationInfo validation = doValidate();
        boolean emptyInput = getInspectionResultAsStr().isEmpty() || getOutputAsStr().isEmpty();
        boolean goodRegex = FileChecker.checkRegexp(getFilterAsStr());
        if (validation == null && !emptyInput && goodRegex) {
            //check if output folder exists and contains files
            Path outDir = Paths.get(getOutputAsStr());
            try {
                if (Files.exists(outDir) && Files.list(outDir).count() > 0) {
                    int message = Messages.showOkCancelDialog("Some files may be overwritten. Do you want to continue?", "The Output Directory Already Contains Files", null);
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
                    filter(indicator, true);
                }
            });
            saveState();
            return EXIT;
        } else {
            validationFlag = true;
            validation = doValidate();
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
        inspectionResult.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.baseline"));
        filter.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.filter"));
    }

    private void saveState() {
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.baseline", inspectionResult.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.filter", filter.getText());
        filter.addTextAndSave();
    }

    private XmlDiffResult filter(ProgressIndicator indicator, boolean out) {
        XmlDiffResult result = null;
        try {
            if (out) {
                result = XmlDiff.filterFolder(getInspectionResultAsStr(), getOutputAsStr(), getFilterAsStr(), indicator);
            } else {
                result = XmlDiff.filterFolder(getInspectionResultAsStr(), null, getFilterAsStr(), indicator);
            }
            if (!indicator.isCanceled() && out) {
                sendNotification(result);
            }
        } catch (AccessDeniedException e) {
            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
        } catch (Exception e) {
            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
        }
        return result;
    }

    private void preview() {
        if (resultsPreview.isVisible()) {
            resultsPreview.setVisible(false);
        }
        previewAlarm.cancelAllRequests();
        boolean goodRegex = FileChecker.checkRegexp(getFilterAsStr());
        if (doValidate() == null && FileChecker.checkInfo(inputInfo) && goodRegex) {
            previewAlarm.addRequest(() -> {
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Filtering") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        XmlDiffResult result = filter(indicator, false);
                        resultsPreview.setText("<html><br>  Filtered count: " + result.filteredCount + "</html>");
                        resultsPreview.setVisible(true);
                    }
                });
            }, 2000);
        }
    }

    private void installLayout() {
        VerticalLayout verticalLayout = new VerticalLayout(2);
        setLayout(verticalLayout);
        verticalLayout.addLayoutComponent(null, inspectionResultLabel);
        verticalLayout.addLayoutComponent(null, buttonContainer);
        verticalLayout.addLayoutComponent(null, inputInfo);
        verticalLayout.addLayoutComponent(null, filterLabel);
        verticalLayout.addLayoutComponent(null, filter.getWrapper());
        verticalLayout.addLayoutComponent(null, outputLabel);
        verticalLayout.addLayoutComponent(null, output);
        verticalLayout.addLayoutComponent(null, resultsPreview);
    }

    @Override
    public void dispose() {

    }
}
