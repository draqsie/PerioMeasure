package com.periomeasure.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

/** First card: what the program does in three numbered steps, and the two ways to start. */
final class WelcomePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    WelcomePanel(MainWindow w) {
        Language l = w.model().settings().language;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        int pad = UIScale.scale(20);
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        add(Ui.title(Messages.get(l, Msg.WELCOME_TITLE), 9));
        add(Box.createVerticalStrut(UIScale.scale(16)));
        JPanel steps = Ui.card(null);
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.add(step(1, Messages.get(l, Msg.WELCOME_STEP1)));
        steps.add(Box.createVerticalStrut(UIScale.scale(12)));
        steps.add(step(2, Messages.get(l, Msg.WELCOME_STEP2)));
        steps.add(Box.createVerticalStrut(UIScale.scale(12)));
        steps.add(step(3, Messages.get(l, Msg.WELCOME_STEP3)));
        add(steps);
        add(Box.createVerticalStrut(UIScale.scale(20)));

        add(big(Ui.primary(Messages.get(l, Msg.TB_OPEN), Icons.Kind.OPEN, e -> w.openPhoto())));
        add(Box.createVerticalStrut(UIScale.scale(10)));
        JButton paste = big(Ui.button(Messages.get(l, Msg.TB_PASTE) + "  (Ctrl+V)", Icons.of(Icons.Kind.PASTE),
                e -> w.paste()));
        paste.setToolTipText(Messages.get(l, Msg.TB_PASTE_TIP));
        add(paste);
        add(Box.createVerticalStrut(UIScale.scale(10)));
        JButton demo = big(Ui.button(Messages.get(l, Msg.TB_DEMO) + "  ▾", Icons.of(Icons.Kind.DEMO), null));
        demo.addActionListener(e -> w.demoMenu().show(demo, 0, demo.getHeight()));
        add(demo);
        add(Box.createVerticalStrut(UIScale.scale(10)));
        JButton tour = Ui.flat(Messages.get(l, Msg.WELCOME_TOUR), Icons.Kind.HELP, null, e -> w.startTour());
        tour.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(tour);
        add(Box.createVerticalStrut(UIScale.scale(16)));
        add(Ui.small(Messages.get(l, Msg.WELCOME_PRIVACY)));
        add(Box.createVerticalGlue());
    }

    private static JButton big(JButton b) {
        b.setFont(b.getFont().deriveFont(Font.BOLD, Themes.font(2)));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIScale.scale(48)));
        b.setPreferredSize(new Dimension(UIScale.scale(320), UIScale.scale(48)));
        return b;
    }

    /** A numbered circle followed by a short sentence. */
    private static JPanel step(int n, String text) {
        JPanel row = new JPanel(new BorderLayout(UIScale.scale(12), 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JComponent badge = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                int s = UIScale.scale(30);
                return new Dimension(s, s);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int s = UIScale.scale(30);
                    g2.setColor(Themes.accent());
                    g2.fillOval(0, 0, s, s);
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont().deriveFont(Font.BOLD, Themes.font(1)));
                    String t = Integer.toString(n);
                    int tw = g2.getFontMetrics().stringWidth(t);
                    int th = g2.getFontMetrics().getAscent();
                    g2.drawString(t, (s - tw) / 2, (s + th) / 2 - UIScale.scale(2));
                } finally {
                    g2.dispose();
                }
            }
        };
        JPanel badgeBox = new JPanel(new BorderLayout());
        badgeBox.setOpaque(false);
        badgeBox.add(badge, BorderLayout.NORTH);
        row.add(badgeBox, BorderLayout.WEST);
        row.add(Ui.text(text), BorderLayout.CENTER);
        return row;
    }
}
