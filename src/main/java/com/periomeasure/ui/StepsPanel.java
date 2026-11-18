package com.periomeasure.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

/** Step-by-step guidance with the loupe; texts and button states come from {@link PanelState}. */
final class StepsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient MainWindow window;
    private final transient Language lang;
    private final JLabel stepLabel = new JLabel();
    private final JLabel title = new JLabel();
    private final JTextArea help = Ui.text("");
    private final JLabel points = new JLabel();
    private final JLabel calibration = new JLabel();
    private final JLabel ready = new JLabel();
    private final JTextArea loupeHint = Ui.text("");
    final JButton undo;
    final JButton previous;
    final JButton skip;
    final JButton next;
    final JButton finish;
    final JButton cancel;

    StepsPanel(MainWindow w) {
        this.window = w;
        this.lang = w.model().settings().language;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        stepLabel.setForeground(Color.DARK_GRAY);
        stepLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JLabel l : new JLabel[] {points, calibration, ready}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        ready.setForeground(Ui.OK);
        ready.setFont(ready.getFont().deriveFont(Font.BOLD));

        undo = Ui.button(m(Msg.BTN_UNDO), e -> act(() -> window.model().undo()));
        previous = Ui.button(m(Msg.BTN_PREVIOUS), e -> act(() -> window.model().previous()));
        skip = Ui.button(m(Msg.BTN_SKIP), e -> act(() -> window.model().skip()));
        next = Ui.button(m(Msg.BTN_NEXT), e -> act(() -> window.model().next()));
        next.setFont(next.getFont().deriveFont(Font.BOLD));
        finish = Ui.button(m(Msg.BTN_FINISH), e -> window.finish());
        finish.setFont(finish.getFont().deriveFont(Font.BOLD, 15f));
        finish.setAlignmentX(Component.LEFT_ALIGNMENT);
        finish.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        cancel = Ui.button(m(Msg.BTN_CANCEL), e -> {
            if (window.confirmDiscard()) {
                window.model().newMeasurement();
                window.refresh();
            }
        });

        add(stepLabel);
        add(title);
        add(Box.createVerticalStrut(4));
        add(help);
        add(points);
        add(calibration);
        add(ready);
        add(Box.createVerticalStrut(8));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row.add(undo);
        row.add(previous);
        row.add(skip);
        row.add(next);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height * 2));
        add(row);
        add(Box.createVerticalStrut(4));
        add(finish);
        add(Box.createVerticalStrut(12));
        LoupeView loupe = w.loupe();
        loupe.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(loupe);
        add(loupeHint);
        add(Box.createVerticalStrut(6));
        JTextArea legend = Ui.text(m(Msg.LEGEND));
        legend.setFont(legend.getFont().deriveFont(11f));
        add(legend);
        add(Box.createVerticalStrut(6));
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row2.add(cancel);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(row2);
        add(Box.createVerticalGlue());
        update();
    }

    private String m(Msg msg) {
        return Messages.get(lang, msg);
    }

    private void act(Runnable r) {
        r.run();
        window.refresh();
    }

    void update() {
        if (window.model().session() == null) {
            return;
        }
        int zoom = window.model().settings().zoomFactor;
        PanelState p = PanelState.of(window.model().session(), lang, zoom);
        stepLabel.setText(p.stepLabel);
        title.setText(p.title);
        help.setText(p.help);
        setOptional(points, p.pointsText);
        setOptional(calibration, p.calibrationText);
        setOptional(ready, p.readyText);
        previous.setEnabled(p.previousEnabled);
        next.setVisible(p.nextVisible);
        next.setEnabled(p.nextEnabled);
        skip.setVisible(p.skipVisible);
        finish.setEnabled(p.finishEnabled);
        loupeHint.setText(Messages.get(lang, Msg.LOUPE_HINT, zoom));
        revalidate();
    }

    private static void setOptional(JLabel label, String text) {
        label.setText(text == null ? "" : text);
        label.setVisible(text != null);
    }

    /** Asks before throwing away marked points. */
    static boolean confirm(Component parent, Language l, String question) {
        String[] options = {Messages.get(l, Msg.BTN_YES), Messages.get(l, Msg.BTN_NO)};
        return JOptionPane.showOptionDialog(parent, question, Messages.get(l, Msg.APP_TITLE), JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]) == 0;
    }
}
