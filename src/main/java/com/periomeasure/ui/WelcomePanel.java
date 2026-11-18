package com.periomeasure.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

/** First card: open a photo or a demo face. */
final class WelcomePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    WelcomePanel(MainWindow w) {
        Language l = w.model().settings().language;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(Ui.title(Messages.get(l, Msg.WELCOME_TITLE), 20f));
        add(Box.createVerticalStrut(12));
        add(Ui.text(Messages.get(l, Msg.WELCOME_TEXT)));
        add(Box.createVerticalStrut(16));
        add(big(Ui.button(Messages.get(l, Msg.TB_OPEN), e -> w.openPhoto())));
        add(Box.createVerticalStrut(8));
        JButton demo = big(Ui.button(Messages.get(l, Msg.TB_DEMO), null));
        demo.addActionListener(e -> w.demoMenu().show(demo, 0, demo.getHeight()));
        add(demo);
        add(Box.createVerticalGlue());
    }

    private static JButton big(JButton b) {
        b.setFont(b.getFont().deriveFont(Font.BOLD, 15f));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return b;
    }
}
