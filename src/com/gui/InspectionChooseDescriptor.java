package com.gui;

import com.intellij.codeInspection.InspectionApplication;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;

public class InspectionChooseDescriptor extends FileChooserDescriptor {

    public InspectionChooseDescriptor() {
        super(false, true, false, false, false, false);
    }

    @Override
    public Icon getIcon(VirtualFile file) {
        if (file.isDirectory()) {
            if (file.findChild(InspectionApplication.DESCRIPTIONS + "." + StdFileTypes.XML.getDefaultExtension()) != null) {
                return AllIcons.Nodes.InspectionResults;
            }
        }
        return super.getIcon(file);
    }
}