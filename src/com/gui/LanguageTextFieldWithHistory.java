package com.gui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

public class LanguageTextFieldWithHistory extends LanguageTextField {
    private String myProperty;
    private int myHistorySize = 0;
    private String [] myHistory;
    private int current = 0;
    private JBPanel wrapper;

    public LanguageTextFieldWithHistory(int historySize, String property, Project project, Language language, JBPanel wrapper) {
        super(language, project, "");
        this.wrapper = wrapper;
        myHistorySize = historySize;
        myProperty = property;
        setOneLineMode(true);
        load();
        addKeyStrokes();
    }

    public void setPreviousTextFromHistory() {
        if (myHistory != null) {
            if (!getText().equals(myHistory[current])) {
                setText(myHistory[myHistory.length - 1]);
                current = myHistory.length - 1;
            } else {
                current = (current - 1 + myHistory.length) % myHistory.length;
                setText(myHistory[current]);
            }
        }
    }

    public void setNextTextFromHistory() {
        if (myHistory != null) {
            if (!getText().equals(myHistory[current])) {
                setText(myHistory[myHistory.length - 1]);
                current = myHistory.length - 1;
            } else {
                current = (current + 1) % myHistory.length;
                setText(myHistory[current]);
            }
        }
    }

    private void load() {
        String value = PropertiesComponent.getInstance().getValue(myProperty);
        if (value != null) {
            myHistory = value.split("\n");
            current = myHistory.length - 1;
        }
    }

    public void addTextAndSave() {
        if (!getText().isEmpty() && (myHistory == null || !getText().equals(myHistory[myHistory.length - 1]))) {
            String valueToStore;
            if (myHistory != null) {
                if (myHistory.length + 1 > myHistorySize) {
                    valueToStore = StringUtil.join(Arrays.copyOfRange(myHistory, 1, myHistory.length), "\n") + "\n" + getText();
                } else {
                    valueToStore = StringUtil.join(myHistory, "\n") + "\n" + getText();
                }
            } else {
                valueToStore = getText();
            }
            PropertiesComponent.getInstance().setValue(myProperty, valueToStore);
        }
    }

    public JPanel getWrapper() {
        return wrapper;
    }
    public void addToWrapper() {
        wrapper.add(this);
    }

    protected void addKeyStrokes() {
        wrapper.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("UP"), "previous");
        wrapper.getActionMap().put("previous", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setPreviousTextFromHistory();
            }
        });
        wrapper.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("DOWN"), "next");
        wrapper.getActionMap().put("next", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setNextTextFromHistory();
            }
        });
    }
}
