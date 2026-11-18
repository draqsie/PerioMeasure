package com.periomeasure.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;

/** Small Swing helpers shared by the panels. */
final class Ui {

    static final Color WARNING = new Color(150, 70, 0);
    static final Color OK = new Color(0, 110, 0);

    private Ui() {
    }

    /** Wrapping, read-only text that looks like a label. */
    static JTextArea text(String s) {
        JTextArea t = new JTextArea(s);
        t.setEditable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setFocusable(false);
        t.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        t.setFont(new JLabel().getFont());
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    static JLabel title(String s, float size) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD, size));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JButton button(String s, ActionListener a) {
        JButton b = new JButton(s);
        if (a != null) {
            b.addActionListener(a);
        }
        return b;
    }
}
