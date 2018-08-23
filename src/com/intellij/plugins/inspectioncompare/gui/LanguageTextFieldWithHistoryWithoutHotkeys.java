package com.intellij.plugins.inspectioncompare.gui;

import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBPanel;

class LanguageTextFieldWithHistoryWithoutHotkeys extends LanguageTextFieldWithHistory {

    LanguageTextFieldWithHistoryWithoutHotkeys(int historySize, String property, Project project, Language language, JBPanel wrapper) {
        super(historySize, property, project, language, wrapper);
    }

    @Override
    protected void addKeyStrokes() {
    }
}
