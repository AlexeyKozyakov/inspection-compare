package com.gui;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;

public class LanguageTextFieldWithHistoryWithoutHotkeys extends LanguageTextFieldWithHistory {

    public LanguageTextFieldWithHistoryWithoutHotkeys(int historySize, String property, Project project, Language language, JBPanel wrapper) {
        super(historySize, property, project, language, wrapper);
    }

    @Override
protected void addKeyStrokes() {
    }
}
