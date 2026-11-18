package com.periomeasure.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import com.formdev.flatlaf.util.UIScale;

/** Shared Swing building blocks so every panel looks the same in both themes. */
final class Ui {

    private Ui() {
    }

    /** Wrapping, read-only text that looks like a label (theme font). */
    static JTextArea text(String s) {
        JTextArea t = new JTextArea(s);
        t.setEditable(false);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setOpaque(false);
        t.setFocusable(false);
        t.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        Font f = UIManager.getFont("defaultFont");
        if (f == null) {
            f = new JLabel().getFont();
        }
        t.setFont(f);
        t.setForeground(UIManager.getColor("Label.foreground"));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    static JTextArea small(String s) {
        JTextArea t = text(s);
        t.setFont(t.getFont().deriveFont(Themes.font(-2)));
        t.setForeground(Themes.muted());
        return t;
    }

    static JLabel title(String s, float delta) {
        JLabel l = new JLabel(s);
        l.setFont(l.getFont().deriveFont(Font.BOLD, Themes.font(delta)));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JButton button(String s, ActionListener a) {
        return button(s, null, a);
    }

    static JButton button(String s, Icon icon, ActionListener a) {
        JButton b = new JButton(s, icon);
        b.setIconTextGap(UIScale.scale(6));
        if (a != null) {
            b.addActionListener(a);
        }
        return b;
    }

    /** Main call-to-action: accent background, white bold text. */
    static JButton primary(String s, Icons.Kind icon, ActionListener a) {
        JButton b = button(s, icon == null ? null : Icons.of(icon, Color.WHITE), a);
        b.setBackground(Themes.accent());
        b.setForeground(Color.WHITE);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.putClientProperty("JButton.buttonType", "roundRect");
        return b;
    }

    /** Flat button for the header bar. */
    static JButton flat(String s, Icons.Kind icon, String tooltip, ActionListener a) {
        JButton b = button(s, icon == null ? null : Icons.of(icon), a);
        b.putClientProperty("JButton.buttonType", "borderless");
        b.setToolTipText(tooltip);
        return b;
    }

    /** Rounded "card" surface with a thin border. */
    static JPanel card(LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int arc = UIScale.scale(14);
                    g2.setColor(Themes.card());
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                    g2.setColor(Themes.cardBorder());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                } finally {
                    g2.dispose();
                }
            }
        };
        p.setOpaque(false);
        int pad = UIScale.scale(14);
        p.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }
}
