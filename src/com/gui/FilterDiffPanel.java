package com.gui;

import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.ui.*;
import com.intellij.ui.components.*;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.Alarm;
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
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
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
    private JBLabel addedWarningsLabel = new JBLabel("Added warnings output");
    private JBLabel removedWarningsLabel = new JBLabel("Removed warnings output");
    private JBLabel replaceFromLabel = new JBLabel("Find what:");
    private JBLabel replaceToLabel = new JBLabel("Replace with:");
    private JBLabel filterLabel = new JBLabel("Filter by substring:");
    private TextFieldWithBrowseButton baseline = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton updated = new TextFieldWithBrowseButton();
    private LanguageTextField filter;
    private TextFieldWithBrowseButton addedWarnings = new TextFieldWithBrowseButton();
    private TextFieldWithBrowseButton removedWarnings = new TextFieldWithBrowseButton();
    private JBCheckBox normalizeCheckBox = new JBCheckBox();
    private JBCheckBox filterCheckbox = new JBCheckBox();
    private JButton swapButton = new JButton();
    private JBPanel buttonContainer = new JBPanel();
    private JBPanel checkboxContainer = new JBPanel();
    private JBPanel normalizeContainer = new JBPanel();
    private JBPanel filterContainer = new JBPanel();
    private JBLabel resultsPreview = new JBLabel();
    private LanguageTextField replaceFrom;
    private JBTextField replaceTo = new JBTextField();
    private Path basePath;
    private Path updatedPath;
    private boolean validationFlag;
    private XmlDiffResult result = new XmlDiffResult();
    private Alarm previewAlarm = new Alarm(this);
    public FilterDiffPanel(Project project) {
        this.project = project;
        setPreferredSize(new Dimension(800, 540));
        //hotkeys
        normalizeCheckBox.setMnemonic(KeyEvent.VK_N);
        filterCheckbox.setMnemonic(KeyEvent.VK_F);
        swapButton.setMnemonic(KeyEvent.VK_S);
        baseInfo.setVisible(false);
        baseInfo.setFontColor(UIUtil.FontColor.BRIGHTER);
        resultsPreview.setVisible(false);
        resultsPreview.setFontColor(UIUtil.FontColor.BRIGHTER);
        updatedInfo.setVisible(false);
        updatedInfo.setFontColor(UIUtil.FontColor.BRIGHTER);
        replaceFrom = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        replaceTo.setBackground(replaceFrom.getBackground());
        initButton();
        baseline.addBrowseFolderListener(null, "Select directory which contains baseline inspection results", project, new InspectionChooseDescriptor());
        updated.addBrowseFolderListener(null, "Select directory which contains updated inspection results", project, new InspectionChooseDescriptor());
        addPathListeners();
        addedWarnings.addBrowseFolderListener(null, "Select added warnings output directory", project, new InspectionChooseDescriptor());
        removedWarnings.addBrowseFolderListener(null, "Select removed warnings output directory", project, new InspectionChooseDescriptor());
        replaceFrom.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
                preview();
            }
        });
        replaceTo.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                preview();
            }
        });
        initFilter();
        initNormalize();
        HorizontalLayout checkboxLayout = new HorizontalLayout(30);
        checkboxContainer.add(normalizeCheckBox);
        checkboxContainer.add(filterCheckbox);
        checkboxContainer.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
        checkboxContainer.setLayout(checkboxLayout);
        checkboxLayout.addLayoutComponent(normalizeCheckBox, null);
        checkboxLayout.addLayoutComponent(filterCheckbox, null);
        add(baselineLabel);
        add(baseline);
        add(baseInfo);
        add(buttonContainer);
        add(updatedLabel);
        add(updated);
        add(updatedInfo);
        add(checkboxContainer);
        add(normalizeContainer);
        add(filterContainer);
        add(addedWarningsLabel);
        add(addedWarnings);
        add(removedWarningsLabel);
        add(removedWarnings);
        add(resultsPreview);
        installLayout();
        Disposer.register(this, baseline);
        Disposer.register(this, updated);
        Disposer.register(this, addedWarnings);
        Disposer.register(this, removedWarnings);
        loadState();
        init();
    }

    private void generateOutPaths() {
        String baseFilename = (basePath == null || basePath.getFileName() == null) ? "" : basePath.getFileName().toString();
        String updatedFilename = (updatedPath == null || updatedPath.getFileName() == null) ? "" : updatedPath.getFileName().toString();
        Path parent = null;
        if (basePath != null && basePath.getParent() != null) {
            parent = basePath.getParent();
        }
        else if (updatedPath != null) {
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
            if (!FileChecker.checkRegexp(getFilterAsStr())) {
                return new ValidationInfo("Syntax error in regex", filter);
            }
            if (!FileChecker.checkRegexp(replaceFrom.getText())) {
                return new ValidationInfo("Syntax error in regex", replaceFrom);
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
        boolean goodRegex = (!filterCheckbox.isSelected() || FileChecker.checkRegexp(getFilterAsStr())) && (!normalizeCheckBox.isSelected() || FileChecker.checkRegexp(replaceFrom.getText()));
        if (validation == null && !emptyInput && goodRegex) {
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
                    diff(indicator, true);
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
                        "<a href=\"added\">Open added</a>&nbsp;&nbsp;&nbsp;&nbsp;" +
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
        getFocusComponent().grabFocus();
    }

    @Override
    public TextFieldWithBrowseButton getLastField() {
        return removedWarnings;
    }

    private void saveState() {
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.baseline", baseline.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.updated", updated.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.filter", filter.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.replaceFrom", replaceFrom.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.replaceTo", replaceTo.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.filterCheckbox", filterCheckbox.isSelected());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.normalizeCheckbox", normalizeCheckBox.isSelected());
    }

    private void loadState() {
        baseline.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.baseline"));
        updated.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.updated"));
        filter.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.filter"));
        replaceFrom.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.replaceFrom"));
        replaceTo.setText(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.replaceTo"));
        if (!filter.getText().isEmpty()) {
            filterCheckbox.setSelected(Boolean.valueOf(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.filterCheckbox")));
        }
        if (!replaceFrom.getText().isEmpty() || !replaceTo.getText().isEmpty()) {
            normalizeCheckBox.setSelected(Boolean.valueOf(PropertiesComponent.getInstance().getValue("Inspection.Compare.Plugin.normalizeCheckbox")));
        }
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

    private void diff(ProgressIndicator indicator, boolean out) {
        try {
            if (out) {
                result = XmlDiff.compareFolders(getBaseAsStr(), getUpdatedAsStr(), getAddedWarningsAsStr(),
                        getRemovedWarningsAsStr(), filterCheckbox.isSelected() ? getFilterAsStr() : "", normalizeCheckBox.isSelected() ? replaceFrom.getText() : "",
                        normalizeCheckBox.isSelected() ? replaceTo.getText() : "", indicator);
            } else {
                result = XmlDiff.compareFolders(getBaseAsStr(), getUpdatedAsStr(), null, null, filterCheckbox.isSelected() ? getFilterAsStr() : "", normalizeCheckBox.isSelected() ? replaceFrom.getText() : "",
                        normalizeCheckBox.isSelected() ? replaceTo.getText() : "", indicator);
            }
            if (!indicator.isCanceled() && out) {
                sendNotification(result);
            }
        } catch (AccessDeniedException e) {
            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
        } catch (Exception e) {
            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
        }
    }

    private void preview() {
        if (resultsPreview.isVisible()) {
            resultsPreview.setVisible(false);
        }
        previewAlarm.cancelAllRequests();
        boolean goodRegex = (!filterCheckbox.isSelected() || FileChecker.checkRegexp(getFilterAsStr())) && (!normalizeCheckBox.isSelected() || FileChecker.checkRegexp(replaceFrom.getText()));
        if (doValidate() == null && FileChecker.checkInfo(baseInfo) && FileChecker.checkInfo(updatedInfo) && goodRegex) {
            previewAlarm.addRequest(() -> {
                ProgressManager.getInstance().run(new Task.Backgroundable(project, "Comparing") {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        diff(indicator, false);
                        resultsPreview.setText("<html><br>  Added warnings: " + result.added + "<br>" +
                                "  Removed warnings: " + result.removed + "</html>");
                        resultsPreview.setVisible(true);
                    }
                });
            }, 2000);
        }
    }

    private void installLayout() {
        VerticalLayout layout = new VerticalLayout(2);
        layout.addLayoutComponent(baselineLabel, null);
        layout.addLayoutComponent(baseline, null);
        layout.addLayoutComponent(baseInfo, null);
        layout.addLayoutComponent(buttonContainer, null);
        layout.addLayoutComponent(updatedLabel, null);
        layout.addLayoutComponent(updated, null);
        layout.addLayoutComponent(updatedInfo, null);
        layout.addLayoutComponent(checkboxContainer, null);
        layout.addLayoutComponent(normalizeContainer, null);
        layout.addLayoutComponent(filterContainer, null);
        layout.addLayoutComponent(addedWarningsLabel, null);
        layout.addLayoutComponent(addedWarnings, null);
        layout.addLayoutComponent(removedWarningsLabel, null);
        layout.addLayoutComponent(removedWarnings, null);
        layout.addLayoutComponent(resultsPreview, null);
        setLayout(layout);
    }

    private void init() {
        basePath = Paths.get(getBaseAsStr());
        updatedPath = Paths.get(getUpdatedAsStr());
        checkFolders();
        preview();
    }

    private void initFilter() {
        filter = new LanguageTextField(RegExpLanguage.INSTANCE, project, "");
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
                preview();
            }
        });
        filterCheckbox.setText("Filter");
        addCheckboxListener(filterCheckbox, filterContainer, filter);
        filterContainer.add(filterLabel);
        filterContainer.add(filter);
        filterContainer.setVisible(false);
        VerticalLayout filterLayout = new VerticalLayout(2);
        filterLayout.addLayoutComponent(filterLabel, null);
        filterLayout.addLayoutComponent(filter, null);
        filterContainer.setLayout(filterLayout);
        filterContainer.setBorder(BorderFactory.createEmptyBorder(0,16,16,1));
    }

    private void initNormalize() {
        normalizeCheckBox.setText("Normalize");
        addCheckboxListener(normalizeCheckBox, normalizeContainer, replaceFrom);
        normalizeContainer.add(replaceFromLabel);
        normalizeContainer.add(replaceFrom);
        normalizeContainer.add(replaceToLabel);
        normalizeContainer.add(replaceTo);
        normalizeContainer.setBorder(BorderFactory.createEmptyBorder(0,16,16,1));
        VerticalLayout containerLayout = new VerticalLayout(2);
        normalizeContainer.setLayout(containerLayout);
        containerLayout.addLayoutComponent(replaceFromLabel, null);
        containerLayout.addLayoutComponent(replaceFrom, null);
        containerLayout.addLayoutComponent(replaceToLabel, null);
        containerLayout.addLayoutComponent(replaceTo, null);
        normalizeContainer.setVisible(false);
    }

    private void addCheckboxListener(JBCheckBox checkbox, JBPanel fieldsContainer, LanguageTextField firstField) {
        checkbox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                fieldsContainer.setVisible(true);
                firstField.grabFocus();
            } else {
                fieldsContainer.setVisible(false);
            }
            preview();
        });
    }

    private void initButton() {
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
        swapButton.addActionListener(actionEvent -> {
            String tmp;
            tmp = getBaseAsStr();
            baseline.setText(getUpdatedAsStr());
            updated.setText(tmp);
        });
        buttonContainer.setLayout(new BorderLayout(5, 5));
        buttonContainer.add(swapButton, BorderLayout.LINE_START);
    }

    private void addPathListeners() {
        baseline.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!getBaseAsStr().isEmpty()) {
                    basePath = Paths.get(baseline.getText());
                    FileChecker.setInfo(baseline.getTextField(), baseInfo);
                    if (!getUpdatedAsStr().isEmpty()) {
                        updatedPath = Paths.get(getUpdatedAsStr());
                        generateOutPaths();
                    }
                    preview();
                }
            }
        });
        updated.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!getUpdatedAsStr().isEmpty()) {
                    updatedPath = Paths.get(updated.getText());
                    FileChecker.setInfo(updated.getTextField(), updatedInfo);
                    if (!getBaseAsStr().isEmpty()) {
                        basePath = Paths.get(getBaseAsStr());
                        generateOutPaths();
                    }
                    preview();
                }
            }
        });
    }

    @Override
    public void dispose() {

    }
}
