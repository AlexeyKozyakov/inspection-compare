package com.gui;

import com.intellij.ui.*;
import com.intellij.ui.components.*;
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
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.intellij.openapi.ui.Messages.OK;

public class FilterDiffPanel extends JBPanel implements DialogTab, Disposable {
    private Project project;
    private JBLabel baselineLabel = new JBLabel("Baseline inspection result");
    private JBLabel updatedLabel = new JBLabel("Updated inspection result");
    private JBLabel baseInfo = new JBLabel();
    private JBLabel updatedInfo = new JBLabel();
    private JBLabel filterLabel = new JBLabel("Filter");
    private JBLabel addedWarningsLabel = new JBLabel("Added warnings output");
    private JBLabel removedWarningsLabel = new JBLabel("Removed warnings output");
    private JBLabel replaceFromLabel = new JBLabel("Find what:");
    private JBLabel replaceToLabel = new JBLabel("Replace with:");
    private TextFieldWithBrowseButton baseline = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton updated = new TextFieldWithBrowseButton();
    private LanguageTextField filter;
    private TextFieldWithBrowseButton addedWarnings = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton removedWarnings = new TextFieldWithBrowseButton();
    private JBCheckBox checkBox = new JBCheckBox();
    private JButton swapButton = new JButton();
    private JBPanel buttonContainer = new JBPanel();
    private JBPanel replaceContainer = new JBPanel();
    private LanguageTextField replaceFrom;
    private JBTextField replaceTo;
    private Path basePath;
    private Path updatedPath;
    private boolean validationFlag;
    private XmlDiffResult result = new XmlDiffResult();
    public FilterDiffPanel(Project project) {
        baseInfo.setVisible(false);
        baseInfo.setFontColor(UIUtil.FontColor.BRIGHTER);
        updatedInfo.setVisible(false);
        updatedInfo.setFontColor(UIUtil.FontColor.BRIGHTER);
        this.project = project;
        filter = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        replaceFrom = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        replaceTo = new JBTextField();
        replaceTo.setBackground(replaceFrom.getBackground());
        loadState();
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
            tmp = getBaseAsStr();
            baseline.setText(getUpdatedAsStr());
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
                FileChecker.setInfo(updated.getTextField(), updatedInfo);
                if (!getBaseAsStr().isEmpty()) {
                    basePath = Paths.get(getBaseAsStr());
                    generateOutPaths();
                }
            }
        });
        baseline.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                basePath = Paths.get(baseline.getText());
                FileChecker.setInfo(baseline.getTextField(), baseInfo);
                if (!getUpdatedAsStr().isEmpty()) {
                    updatedPath = Paths.get(getUpdatedAsStr());
                    generateOutPaths();
                }
            }
        });
        checkBox.setText("Normalize");
        checkBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                replaceContainer.setVisible(true);
                replaceFrom.grabFocus();
            } else {
                replaceContainer.setVisible(false);
                grabFocus();
            }
        });
        replaceContainer.add(replaceFromLabel);
        replaceContainer.add(replaceFrom);
        replaceContainer.add(replaceToLabel);
        replaceContainer.add(replaceTo);
        replaceContainer.setBorder(BorderFactory.createEmptyBorder(8,16,8,1));

        VerticalLayout containerLayout = new VerticalLayout(2);
        replaceContainer.setLayout(containerLayout);
        containerLayout.addLayoutComponent(replaceFromLabel, null);
        containerLayout.addLayoutComponent(replaceFrom, null);
        containerLayout.addLayoutComponent(replaceToLabel, null);
        containerLayout.addLayoutComponent(replaceTo, null);
        replaceContainer.setVisible(false);
        setPreferredSize(new Dimension(800, 540));
        VerticalLayout layout = new VerticalLayout(2);
        add(baselineLabel);
        add(baseline);
        add(baseInfo);
        add(buttonContainer);
        add(updatedLabel);
        add(updated);
        add(updatedInfo);
        add(checkBox);
        add(replaceContainer);
        add(filterLabel);
        add(filter);
        add(addedWarningsLabel);
        add(addedWarnings);
        add(removedWarningsLabel);
        add(removedWarnings);
        layout.addLayoutComponent(baselineLabel, null);
        layout.addLayoutComponent(baseline, null);
        layout.addLayoutComponent(baseInfo, null);
        layout.addLayoutComponent(buttonContainer, null);
        layout.addLayoutComponent(updatedLabel, null);
        layout.addLayoutComponent(updated, null);
        layout.addLayoutComponent(updatedInfo, null);
        layout.addLayoutComponent(checkBox, null);
        layout.addLayoutComponent(replaceContainer, null);
        layout.addLayoutComponent(filterLabel, null);
        layout.addLayoutComponent(filter, null);
        layout.addLayoutComponent(addedWarningsLabel, null);
        layout.addLayoutComponent(addedWarnings, null);
        layout.addLayoutComponent(removedWarningsLabel, null);
        layout.addLayoutComponent(removedWarnings, null);
        setLayout(layout);
        Disposer.register(this, baseline);
        Disposer.register(this, updated);
        Disposer.register(this, addedWarnings);
        Disposer.register(this, removedWarnings);
        basePath = Paths.get(getBaseAsStr());
        updatedPath = Paths.get(getUpdatedAsStr());
        checkFolders();
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
        if (!getBaseAsStr().isEmpty() && Files.notExists(basePath)) {
            return new ValidationInfo("Baseline folder doesn't exist", baseline.getTextField());
        }
        if (!getUpdatedAsStr().isEmpty() && Files.notExists(updatedPath)) {
            return new ValidationInfo("Updated folder doesn't exist", updated.getTextField());
        }
        if (!getBaseAsStr().isEmpty() && !FileChecker.checkFile(basePath)) {
            return new ValidationInfo("Baseline folder doesn't contain inspection results", baseline.getTextField());
        }
        if (!getUpdatedAsStr().isEmpty() && !FileChecker.checkFile(updatedPath)) {
            return new ValidationInfo("Updated folder doesn't contain inspection results", updated.getTextField());
        }
        if (!getUpdatedAsStr().isEmpty() && !FileChecker.checkFile(updatedPath)) {
            return new ValidationInfo("Updated folder doesn't contain inspection results", updated.getTextField());
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
            //check if output folders exist and contain files
            Path addedDir = Paths.get(getAddedWarningsAsStr());
            Path removedDir = Paths.get(getAddedWarningsAsStr());
            try {
                if (Files.exists(addedDir) && Files.list(addedDir).count() > 0 || Files.exists(removedDir) && Files.list(removedDir).count() > 0) {

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
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Comparing") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        result = XmlDiff.compareFolders(getBaseAsStr(), getUpdatedAsStr(), getAddedWarningsAsStr(),
                                getRemovedWarningsAsStr(), getFilterAsStr(), checkBox.isSelected() ? replaceFrom.getText() : "",
                                checkBox.isSelected() ? replaceTo.getText() : "", indicator);
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
            validation = doValidate();
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
    private void sendNotification(XmlDiffResult result) {
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

    @Override
    public void clear() {
        baseline.setText("");
        updated.setText("");
        filter.setText("");
        addedWarnings.setText("");
        removedWarnings.setText("");
        replaceFrom.setText("");
        replaceTo.setText("");
        grabFocus();
    }

    private void saveState() {
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.baseline", baseline.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.updated", updated.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.addedWarnings", addedWarnings.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.removedWarnings", removedWarnings.getText());
    }

    private void loadState() {
        baseline.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.baseline"));
        updated.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.updated"));
        addedWarnings.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.addedWarnings"));
        removedWarnings.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.removedWarnings"));
    }

    private void checkFolders(){
        FileChecker.setInfo(baseline.getTextField(), baseInfo);
        FileChecker.setInfo(updated.getTextField(), updatedInfo);

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

    @Override
    public void dispose() {

    }
}
