package com.gui;

import com.intellij.openapi.ui.ValidationInfo;

import javax.swing.*;

public interface DialogTab {
    int CONTINUE = 0;
    int EXIT = 1;
    public ValidationInfo doValidate();
    public int run();
    public JComponent getFocusComponent();
}
