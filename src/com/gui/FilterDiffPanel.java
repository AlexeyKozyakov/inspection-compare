package com.gui;

import com.inspectionDiff.OfflineViewer;
import com.inspectionDiff.XmlDiff;
import com.inspectionDiff.XmlDiffResult;
import com.intellij.codeInspection.InspectionApplication;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
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
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.regexp.RegExpFileType;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.intellij.openapi.ui.Messages.OK;

public class FilterDiffPanel extends JBPanel implements DialogTab {
    private Project project;
    private JBLabel baselineLabel = new JBLabel("Baseline inspection result");
    private JBLabel updatedLabel = new JBLabel("Updated inspection result");
    private JBLabel filterLabel = new JBLabel("Filter");
    private JBLabel addedWarningsLabel = new JBLabel("Added warnings output");
    private JBLabel removedWarningsLabel = new JBLabel("Removed warnings output");
    private TextFieldWithBrowseButton baseline = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton updated = new TextFieldWithBrowseButton();
    private EditorTextField filter;
    private TextFieldWithBrowseButton addedWarnings = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton removedWarnings = new TextFieldWithBrowseButton();
    private JButton swapButton = new JButton();
    private JBPanel buttonContainer = new JBPanel();
    private Path basePath;
    private Path updatedPath;
    private boolean validationFlag = false;
    private XmlDiffResult result = new XmlDiffResult();
    public FilterDiffPanel(Project project) {
        this.project = project;
        filter = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        Image iconImage = null;
        try {
            iconImage = ImageIO.read(getClass().getResource("resources/swap1.png"));
        } catch (Exception e) {
            System.err.println("Cannot load button icon");
        }
        if (iconImage != null) {
            iconImage = dye((BufferedImage) iconImage, JBColor.foreground()).getScaledInstance(30, 38, Image.SCALE_DEFAULT);
            swapButton.setIcon(new ImageIcon(iconImage));
        }
        swapButton.setPreferredSize(new Dimension(50, 50));
        swapButton.setToolTipText("Swap input folders");
        buttonContainer.setLayout(new BorderLayout(5, 5));
        buttonContainer.add(swapButton, BorderLayout.LINE_START);
        swapButton.addActionListener(actionEvent -> {
            String tmp;
            tmp = baseline.getText();
            baseline.setText(updated.getText());
            updated.setText(tmp);
            grabFocus();
        });

        baseline.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        updated.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        addedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        removedWarnings.addBrowseFolderListener(new TextBrowseFolderListener(new InspectionChooseDescriptor()));
        updated.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                updatedPath = Paths.get(updated.getText());
                if (!baseline.getText().isEmpty()) {
                    basePath = Paths.get(baseline.getText());
                    generateOutPaths();
                }
            }
        });
        baseline.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                basePath = Paths.get(baseline.getText());
                if (!updated.getText().isEmpty()) {
                    updatedPath = Paths.get(updated.getText());
                    generateOutPaths();
                }
            }
        });
        setPreferredSize(new Dimension(800, 500));
        VerticalLayout layout = new VerticalLayout(1);
        add(baselineLabel);
        add(baseline);
        add(buttonContainer);
        add(updatedLabel);
        add(updated);
        add(filterLabel);
        add(filter);
        add(addedWarningsLabel);
        add(addedWarnings);
        add(removedWarningsLabel);
        add(removedWarnings);
        layout.addLayoutComponent(baselineLabel, null);
        layout.addLayoutComponent(baseline, null);
        layout.addLayoutComponent(buttonContainer, null);
        layout.addLayoutComponent(updatedLabel, null);
        layout.addLayoutComponent(updated, null);
        layout.addLayoutComponent(filterLabel, null);
        layout.addLayoutComponent(filter, null);
        layout.addLayoutComponent(addedWarningsLabel, null);
        layout.addLayoutComponent(addedWarnings, null);
        layout.addLayoutComponent(removedWarningsLabel, null);
        layout.addLayoutComponent(removedWarnings, null);
        setLayout(layout);
    }
    private void generateOutPaths() {
        String baseFilename = (basePath == null || basePath.getFileName() == null) ? "" : basePath.getFileName().toString();
        String updatedFilename = (updatedPath == null || updatedPath.getFileName() == null) ? "" : updatedPath.getFileName().toString();
        Path parent;
        if (basePath != null && basePath.getParent() != null) {
            parent = basePath.getParent();
        }
        else {
            parent = updatedPath.getParent();
        }
        if (parent != null) {
            addedWarnings.setText(parent.resolve("from_" + baseFilename + "_to_" + updatedFilename).toString());
            removedWarnings.setText(parent.resolve("from_" + updatedFilename + "_to_" + baseFilename).toString());
        }
    }

    @Override
    public ValidationInfo doValidate() {
        //don't show message about empty fields before button is pressed
        if (validationFlag) {
            if (getBaseAsStr().isEmpty()) {
                return new ValidationInfo("Choose baseline folder", baseline.getTextField());
            }
            if (getUpdatedAsStr().isEmpty()) {
                return new ValidationInfo("Choose updated folder", updated.getTextField());
            }
            if (getAddedWarningsAsStr().isEmpty()) {
                return new ValidationInfo("Choose added warnings out folder", addedWarnings.getTextField());
            }
            if (getRemovedWarningsAsStr().isEmpty()) {
                return new ValidationInfo("Choose removed warnings out folder", removedWarnings.getTextField());
            }
        }
        if (!Files.exists(Paths.get(getBaseAsStr())) ) {
            return new ValidationInfo("Baseline folder does not exist", baseline.getTextField());
        }
        if (!Files.exists(Paths.get(getUpdatedAsStr())) ) {
            return new ValidationInfo("Updated folder does not exist", updated.getTextField());
        }
        if (!getBaseAsStr().isEmpty() && getBaseAsStr().equals(getUpdatedAsStr())) {
            return new ValidationInfo("Choose different baseline and updated folders", baseline.getTextField());
        }
        if (!getAddedWarningsAsStr().isEmpty() && getAddedWarningsAsStr().equals(getRemovedWarningsAsStr())) {
            return new ValidationInfo("Choose different output folders", addedWarnings.getTextField());
        }
        return null;
    }

    @Override
    public int run() {
        ValidationInfo validation = doValidate();
        //check if input fields is empty
        boolean emptyInput = getBaseAsStr().isEmpty() || getUpdatedAsStr().isEmpty() || getAddedWarningsAsStr().isEmpty() || getRemovedWarningsAsStr().isEmpty();
        if (validation == null && !emptyInput) {
            //check if output folders exists and contain files
            Path addedDir = Paths.get(getAddedWarningsAsStr());
            Path removedDir = Paths.get(getAddedWarningsAsStr());
            try {
                if (Files.exists(addedDir) && Files.list(addedDir).count() > 0 || Files.exists(removedDir) && Files.list(removedDir).count() > 0) {

                    int message = Messages.showOkCancelDialog("Some files may be overwritten. Do you want to continue?", "The output directory already contains files", null);
                    if (message != OK) {
                        return CONTINUE;
                    }
                }
            } catch (IOException e) {
                Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Comparing") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        result = XmlDiff.compareFolders(getBaseAsStr(), getUpdatedAsStr(), getAddedWarningsAsStr(),
                                getRemovedWarningsAsStr(), getFilterAsStr(), indicator);
                        sendNotification(result, project);
                    } catch (AccessDeniedException e) {
                        Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
                    } catch (Exception e) {
                        Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
                    }
                }
            });
            return EXIT;
        } else {
            validationFlag = true;
            validation.component.grabFocus();
            return CONTINUE;
        }
    }

    public String getBaseAsStr() {
        return baseline.getText();
    }
    public String getUpdatedAsStr() {
        return updated.getText();
    }
    public String getFilterAsStr() {
        return filter.getText();
    }
    public String getAddedWarningsAsStr() {
        return addedWarnings.getText();
    }
    public String getRemovedWarningsAsStr() {
        return removedWarnings.getText();
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
                VirtualFile added = LocalFileSystem.getInstance().refreshAndFindFileByPath(getAddedWarningsAsStr());
                OfflineViewer.openOfflineView(added, project);
            }
            if (event.getDescription().equals("removed")) {
                VirtualFile removed = LocalFileSystem.getInstance().refreshAndFindFileByPath(getRemovedWarningsAsStr());
                OfflineViewer.openOfflineView(removed, project);
            }
        }));
    }

    @Override
    public JComponent getFocusComponent() {
        return baseline;
    }

    private static BufferedImage dye(BufferedImage image, Color color)
    {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage dyed = UIUtil.createImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dyed.createGraphics();
        g.drawImage(image, 0,0, null);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(color);
        g.fillRect(0,0,w,h);
        g.dispose();
        return dyed;
    }

}
