package gui;

import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import java.awt.*;

public class DialogPanel extends JPanel {

    private JLabel baselineLabel = new JLabel("Baseline inspection result");
    private JLabel updatedLabel = new JLabel("Updated inspection result");
    private JLabel filterLabel = new JLabel("Filter");
    private JLabel addedWarningsLabel = new JLabel("Added warnings output");
    private JLabel removedWarningsLabel = new JLabel("Removed warnings output");
    private JTextField baseline = new JTextField();
    private JTextField updated = new JTextField();
    private JTextField filter = new JTextField();
    private JTextField addedWarnings = new JTextField();
    private JTextField removedWarnings = new JTextField();

    public DialogPanel() {
        add(baselineLabel);
        add(baseline);
        add(updatedLabel);
        add(updated);
        add(filterLabel);
        add(filter);
        add(addedWarningsLabel);
        add(addedWarnings);
        add(removedWarningsLabel);
        add(removedWarnings);
        setLayout(new GridLayout(10, 1));
        setPreferredSize(new Dimension(800, 600));
    }

}
