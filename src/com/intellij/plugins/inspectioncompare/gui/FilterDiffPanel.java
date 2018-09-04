package com.intellij.plugins.inspectioncompare.gui;

import com.intellij.codeEditor.printing.ExportToHTMLSettings;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.plugins.inspectioncompare.diff.XmlDiff;
import com.intellij.plugins.inspectioncompare.diff.XmlDiffResult;
import com.intellij.plugins.inspectioncompare.util.FileChecker;
import com.intellij.plugins.inspectioncompare.util.OfflineViewer;
import com.intellij.ui.*;
import com.intellij.ui.components.*;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.util.Alarm;
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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutionException;

import static com.intellij.openapi.ui.Messages.OK;
import static com.intellij.plugins.inspectioncompare.util.FileChecker.getPathIfValid;

public class FilterDiffPanel extends JBPanel implements DialogTab, Disposable {
    private final Project project;
    private final JBLabel baselineLabel = new JBLabel("Baseline inspection result");
    private final JBLabel updatedLabel = new JBLabel("Updated inspection result");
    private final JBLabel baseInfo = new JBLabel();
    private final JBLabel updatedInfo = new JBLabel();
    private final JBLabel addedWarningsLabel = new JBLabel("Added warnings output");
    private final JBLabel removedWarningsLabel = new JBLabel("Removed warnings output");
    private final JBLabel replaceFromLabel = new JBLabel("Find what:");
    private final JBLabel replaceToLabel = new JBLabel("Replace with:");
    private final JBLabel filterLabel = new JBLabel("Filter by substring:");
    private final TextFieldWithBrowseButton baseline = new TextFieldWithBrowseButton();
    private final TextFieldWithBrowseButton updated = new TextFieldWithBrowseButton();
    private LanguageTextFieldWithHistory filter;
    private final TextFieldWithBrowseButton addedWarnings = new TextFieldWithBrowseButton();
    private final TextFieldWithBrowseButton removedWarnings = new TextFieldWithBrowseButton();
    private final JBCheckBox normalizeCheckBox = new JBCheckBox();
    private final JBCheckBox filterCheckbox = new JBCheckBox();
    private final JButton swapButton = new JButton();
    private final JBPanel buttonContainer = new JBPanel();
    private final JBPanel checkboxContainer = new JBPanel();
    private final JBPanel normalizeContainer = new JBPanel();
    private final JBPanel filterContainer = new JBPanel();
    private final JBLabel resultsPreview = new JBLabel();
    private final LanguageTextFieldWithHistory replaceFrom;
    private final LanguageTextFieldWithHistory replaceTo;
    private final JButton baseLastRes = new JButton();
    private final JButton updatedLastRes = new JButton();
    private Path basePath;
    private Path updatedPath;
    private boolean validationFlag;
    private XmlDiffResult result = new XmlDiffResult();
    private final Alarm previewAlarm = new Alarm(this);
    private final JBPanel baseFieldAndButton = new JBPanel(new BorderLayout());
    private final JBPanel updatedFieldAndButton = new JBPanel(new BorderLayout());
    private Path addedDir;
    private Path removedDir;
    private final String lastDir;

    public FilterDiffPanel(Project project) {
        this.project = project;
        setPreferredSize(new Dimension(800, 540));
        //hotkeys
        normalizeCheckBox.setMnemonic(KeyEvent.VK_N);
        filterCheckbox.setMnemonic(KeyEvent.VK_F);
        swapButton.setMnemonic(KeyEvent.VK_S);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int buttonSize =(int) (60 * (screenSize.width / 3840.0));
        baseLastRes.setText("L");
        baseLastRes.setPreferredSize(new Dimension(buttonSize, buttonSize));
        lastDir = (ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY == null) ? "" : ExportToHTMLSettings.getInstance(project).OUTPUT_DIRECTORY;
        baseLastRes.setToolTipText("Open folder which contains latest exported results " + "(" + lastDir + ")");
        updatedLastRes.setText("L");
        updatedLastRes.setPreferredSize(new Dimension(buttonSize, buttonSize));
        updatedLastRes.setToolTipText("Open folder which contains latest exported results " + "(" + lastDir + ")");
        baseline.getTextField().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK), "latestResult");
        baseline.getTextField().getActionMap().put("latestResult", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                baseLastRes.doClick();
            }
        });
        baseline.getTextField().setToolTipText("Press Alt+L to select the latest inspection result" + "(" + lastDir + ")");
        updated.getTextField().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK), "latestResult");
        updated.getTextField().getActionMap().put("latestResult", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updatedLastRes.doClick();
            }
        });
        updated.getTextField().setToolTipText("Press Alt+L to select the latest inspection result" + "(" + lastDir + ")");
        baseLastRes.setVisible(false);
        updatedLastRes.setVisible(false);
        baseFieldAndButton.add(baseline);
        baseFieldAndButton.add(baseLastRes, BorderLayout.LINE_END);
        updatedFieldAndButton.add(updated);
        updatedFieldAndButton.add(updatedLastRes, BorderLayout.LINE_END);
        baseInfo.setVisible(false);
        baseInfo.setFontColor(UIUtil.FontColor.BRIGHTER);
        resultsPreview.setVisible(false);
        resultsPreview.setFontColor(UIUtil.FontColor.BRIGHTER);
        updatedInfo.setVisible(false);
        updatedInfo.setFontColor(UIUtil.FontColor.BRIGHTER);
        replaceFrom = new LanguageTextFieldWithHistoryWithoutHotkeys(10, "Inspection.Compare.Plugin.normalizeHistoryFrom",
                project, RegExpLanguage.INSTANCE, normalizeContainer);
        replaceTo = new LanguageTextFieldWithHistoryWithoutHotkeys(10, "Inspection.Compare.Plugin.normalizeHistoryTo",
                project, PlainTextLanguage.INSTANCE, normalizeContainer);
        replaceTo.setBackground(replaceFrom.getBackground());
        initButton(buttonSize);
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
        replaceTo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
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
        add(baseFieldAndButton);
        add(baseInfo);
        add(buttonContainer);
        add(updatedLabel);
        add(updatedFieldAndButton);
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
        if (!lastDir.isEmpty() && !lastDir.equals(getBaseAsStr())) {
            baseLastRes.setVisible(true);
        }
        baseLastRes.addActionListener(e -> {
            if (baseLastRes.isVisible()) {
                baseline.setText(lastDir);
                baseLastRes.setVisible(false);
                baseline.grabFocus();
                baseline.getTextField().selectAll();
            }
        });
        if (!lastDir.isEmpty() && !lastDir.equals(getUpdatedAsStr())) {
            updatedLastRes.setVisible(true);
        }
        updatedLastRes.addActionListener(e -> {
            if (updatedLastRes.isVisible()) {
                updated.setText(lastDir);
                updatedLastRes.setVisible(false);
                updated.grabFocus();
                updated.getTextField().selectAll();
            }
        });
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
        if (!getBaseAsStr().isEmpty() && basePath == null) {
            return new ValidationInfo("Invalid path", baseline.getTextField());
        }
        if (!getUpdatedAsStr().isEmpty() && updatedPath == null) {
            return new ValidationInfo("Invalid path", updated.getTextField());
        }
        if (!getAddedWarningsAsStr().isEmpty() && addedDir == null) {
            return new ValidationInfo("Invalid path", addedWarnings.getTextField());
        }
        if (!getRemovedWarningsAsStr().isEmpty() && removedDir == null) {
            return new ValidationInfo("Invalid path", removedWarnings.getTextField());
        }
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
        if (!getBaseAsStr().isEmpty() && (Files.notExists(basePath))) {
            return new ValidationInfo("Baseline folder doesn't exist", baseline.getTextField());
        }
        if (!getUpdatedAsStr().isEmpty() && (Files.notExists(updatedPath))) {
            return new ValidationInfo("Updated folder doesn't exist", updated.getTextField());
        }
        if (!getBaseAsStr().isEmpty() && !FileChecker.checkFile(basePath)) {
            return new ValidationInfo("Baseline folder doesn't contain inspection results", baseline.getTextField());
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
            try {
                if (Files.exists(addedDir) && Files.list(addedDir).count() > 0 || Files.exists(removedDir) && Files.list(removedDir).count() > 0) {

                    int message = Messages.showOkCancelDialog("Some files may be overwritten. Do you want to continue?", "The Output Directory Already Contains Files", null);
                    if (message != OK) {
                        return CONTINUE;
                    }
                }
            }catch (AccessDeniedException e) {
                Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
            } catch (IOException e) {
                Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
            }
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Comparing") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
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

    private String getBaseAsStr() {
        return baseline.getText();
    }
    private String getUpdatedAsStr() {
        return updated.getText();
    }
    private String getFilterAsStr() {
        return filter.getText();
    }
    private String getAddedWarningsAsStr() {
        return addedWarnings.getText();
    }
    private String getRemovedWarningsAsStr() {
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

    private void saveState() {
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.baseline", getBaseAsStr());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.updated", getUpdatedAsStr());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.filter", filter.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.replaceFrom", replaceFrom.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.replaceTo", replaceTo.getText());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.filterCheckbox", filterCheckbox.isSelected());
        PropertiesComponent.getInstance().setValue("Inspection.Compare.Plugin.normalizeCheckbox", normalizeCheckBox.isSelected());
        filter.addTextAndSave();
        replaceFrom.addTextAndSave();
        replaceTo.addTextAndSave();
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
        try {
            FileChecker.setInfo(baseline.getTextField(), basePath, baseInfo);
            FileChecker.setInfo(updated.getTextField(), updatedPath, updatedInfo);
        } catch (ExecutionException e) {
            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
        }

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
                result = XmlDiff.compareFolders(basePath, updatedPath, addedDir,
                        removedDir, filterCheckbox.isSelected() ? getFilterAsStr() : "", normalizeCheckBox.isSelected() ? replaceFrom.getText() : "",
                        normalizeCheckBox.isSelected() ? replaceTo.getText() : "", indicator);
            } else {
                result = XmlDiff.compareFolders(basePath, updatedPath, null, null, filterCheckbox.isSelected() ? getFilterAsStr() : "", normalizeCheckBox.isSelected() ? replaceFrom.getText() : "",
                        normalizeCheckBox.isSelected() ? replaceTo.getText() : "", indicator);
            }
            if (!indicator.isCanceled() && out) {
                sendNotification(result);
            }
        } catch (AccessDeniedException e) {
            Notifications.Bus.notify(new Notification("Plugins notifications", "Error", "Access to folder denied", NotificationType.ERROR));
        } catch (ParserConfigurationException | TransformerException e) {
            Notifications.Bus.notify(new Notification("Plugins notifications", "Error in XML parser", e.toString(), NotificationType.ERROR));
        } catch (IOException | ExecutionException e) {
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
            previewAlarm.addRequest(() -> ProgressManager.getInstance().run(new Task.Backgroundable(project, "Comparing") {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    indicator.setIndeterminate(false);
                    diff(indicator, false);
                    resultsPreview.setText("<html><br>  Added warnings: " + result.added + "<br>" +
                            "  Removed warnings: " + result.removed + "</html>");
                    resultsPreview.setVisible(true);
                }
            }), 2000);
        }
    }

    private void installLayout() {
        VerticalLayout layout = new VerticalLayout(2);
        layout.addLayoutComponent(baselineLabel, null);
        layout.addLayoutComponent(baseFieldAndButton, null);
        layout.addLayoutComponent(baseInfo, null);
        layout.addLayoutComponent(buttonContainer, null);
        layout.addLayoutComponent(updatedLabel, null);
        layout.addLayoutComponent(updatedFieldAndButton, null);
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
        checkFolders();
        preview();
    }

    private void initFilter() {
        filter = new LanguageTextFieldWithHistory(10, "Inspection.Compare.Plugin.filterHistory",
                project, RegExpLanguage.INSTANCE, new JBPanel(new BorderLayout()));
        filter.addToWrapper();
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent event) {
                preview();
            }
        });
        filterCheckbox.setText("Filter");
        addCheckboxListener(filterCheckbox, filterContainer, filter);
        filterContainer.add(filterLabel);
        filterContainer.add(filter.getWrapper());
        filterContainer.setVisible(false);
        VerticalLayout filterLayout = new VerticalLayout(2);
        filterLayout.addLayoutComponent(filterLabel, null);
        filterLayout.addLayoutComponent(filter.getWrapper(), null);
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
        normalizeContainer.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("UP"), "previous");
        normalizeContainer.getActionMap().put("previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                replaceFrom.setPreviousTextFromHistory();
                replaceTo.setPreviousTextFromHistory();
            }
        });
        normalizeContainer.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DOWN"), "next");
        normalizeContainer.getActionMap().put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                replaceFrom.setNextTextFromHistory();
                replaceTo.setNextTextFromHistory();
            }
        });
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

    private void initButton(int size) {
        Image iconImage = null;
        try {
            iconImage = ImageIO.read(getClass().getResource("resources/swap1.png"));
        } catch (IOException e) {
            System.err.println("Cannot load button icon");
        }
        if (iconImage != null) {
            iconImage = dye((BufferedImage) iconImage, JBColor.foreground()).getScaledInstance((int)(size * 0.6), (int)(size * 0.8), Image.SCALE_DEFAULT);
            swapButton.setIcon(new ImageIcon(iconImage));
        }
        swapButton.setPreferredSize(new Dimension(size, size));
        swapButton.setToolTipText("Swap input folders");
        swapButton.addActionListener(actionEvent -> {
            String tmp;
            tmp = getBaseAsStr();
            baseline.setText(getUpdatedAsStr());
            updated.setText(tmp);
            if (previewAlarm.getActiveRequestCount() > 0) {
                previewAlarm.cancelAllRequests();
                int temp = result.added;
                result.added = result.removed;
                result.removed = temp;
                resultsPreview.setText("<html><br>  Added warnings: " + result.added + "<br>" +
                        "  Removed warnings: " + result.removed + "</html>");
                resultsPreview.setVisible(true);
            }
        });
        buttonContainer.setLayout(new BorderLayout(5, 5));
        buttonContainer.add(swapButton, BorderLayout.LINE_START);
    }

    private void addPathListeners() {
        baseline.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!getBaseAsStr().isEmpty()) {
                    basePath = getPathIfValid(getBaseAsStr());
                    try {
                        FileChecker.setInfo(baseline.getTextField(), basePath, baseInfo);
                    } catch (ExecutionException ex) {
                        Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
                    }
                    if (!getUpdatedAsStr().isEmpty()) {
                        updatedPath = getPathIfValid(getUpdatedAsStr());
                        generateOutPaths();
                    }
                    preview();
                } else {
                    if (baseInfo.isVisible()) {
                        baseInfo.setVisible(false);
                    }
                }
                setLast(baseline, baseLastRes);
            }
        });
        updated.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!getUpdatedAsStr().isEmpty()) {
                    updatedPath = getPathIfValid(getUpdatedAsStr());
                    try {
                        FileChecker.setInfo(updated.getTextField(), updatedPath, updatedInfo);
                    } catch (ExecutionException ex) {
                        Notifications.Bus.notify(new Notification("Plugins notifications", "Error", e.toString(), NotificationType.ERROR));
                    }
                    if (!getBaseAsStr().isEmpty()) {
                        basePath = getPathIfValid(getBaseAsStr());
                        generateOutPaths();
                    }
                    preview();
                } else {
                    if (updatedInfo.isVisible()) {
                        updatedInfo.setVisible(false);
                    }
                }
                setLast(updated, updatedLastRes);
            }
        });
        addedWarnings.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!getAddedWarningsAsStr().isEmpty()) {
                    addedDir = getPathIfValid(getAddedWarningsAsStr());
                }
            }
        });
        removedWarnings.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                if (!getRemovedWarningsAsStr().isEmpty()) {
                    removedDir = getPathIfValid(getRemovedWarningsAsStr());
                }
            }
        });
    }

    private void setLast(TextFieldWithBrowseButton updated, JButton updatedLastRes) {
        if (!lastDir.isEmpty() && !lastDir.equals(updated.getText())) {
            if (!updatedLastRes.isVisible()) {
                updatedLastRes.setVisible(true);
            }
        } else {
            if (updatedLastRes.isVisible()) {
                updatedLastRes.setVisible(false);
            }
        }
    }

    @Override
    public void dispose() {

    }
}
