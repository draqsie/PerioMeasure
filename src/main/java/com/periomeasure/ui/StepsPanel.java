package com.periomeasure.ui;

import java.awt.BorderLayout;
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
import javax.swing.JProgressBar;
import javax.swing.JTextArea;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

/** Step-by-step guidance with the loupe; texts and button states come from {@link PanelState}. */
final class StepsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient MainWindow window;
    private final transient Language lang;
    private final JProgressBar progress = new JProgressBar();
    private final JLabel stepLabel = new JLabel();
    private final JLabel title = new JLabel();
    private final JTextArea help = Ui.text("");
    private final JLabel points = new JLabel();
    private final JLabel calibration = new JLabel();
    private final JLabel ready = new JLabel();
    private final JTextArea loupeHint = Ui.small("");
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
        int pad = UIScale.scale(18);
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        stepLabel.setForeground(Themes.muted());
        stepLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setAlignmentX(Component.LEFT_ALIGNMENT);
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIScale.scale(8)));
        progress.setPreferredSize(new Dimension(UIScale.scale(300), UIScale.scale(8)));
        progress.setForeground(Themes.accent());
        title.setFont(title.getFont().deriveFont(Font.BOLD, Themes.font(6)));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JLabel l : new JLabel[] {points, calibration, ready}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        calibration.setForeground(Themes.accent());
        ready.setForeground(Themes.success());
        ready.setFont(ready.getFont().deriveFont(Font.BOLD));

        undo = Ui.button(m(Msg.BTN_UNDO), Icons.of(Icons.Kind.UNDO), e -> act(() -> window.model().undo()));
        previous = Ui.button(m(Msg.BTN_PREVIOUS).replace("◀", "").trim(), Icons.of(Icons.Kind.BACK),
                e -> act(() -> window.model().previous()));
        skip = Ui.button(m(Msg.BTN_SKIP), Icons.of(Icons.Kind.SKIP), e -> act(() -> window.model().skip()));
        next = Ui.button(m(Msg.BTN_NEXT).replace("▶", "").trim(), Icons.of(Icons.Kind.NEXT),
                e -> act(() -> window.model().next()));
        next.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        next.setFont(next.getFont().deriveFont(Font.BOLD));
        finish = Ui.primary(m(Msg.BTN_FINISH), Icons.Kind.CHECK, e -> window.finish());
        finish.setFont(finish.getFont().deriveFont(Font.BOLD, Themes.font(2)));
        finish.setAlignmentX(Component.LEFT_ALIGNMENT);
        finish.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIScale.scale(46)));
        cancel = Ui.flat(m(Msg.BTN_CANCEL), null, null, e -> {
            if (window.confirmDiscard()) {
                window.model().newMeasurement();
                window.refresh();
            }
        });

        add(stepLabel);
        add(Box.createVerticalStrut(UIScale.scale(4)));
        add(progress);
        add(Box.createVerticalStrut(UIScale.scale(10)));
        add(title);
        add(Box.createVerticalStrut(UIScale.scale(8)));
        JPanel helpCard = Ui.card(new BorderLayout());
        helpCard.add(help, BorderLayout.CENTER);
        JPanel status = new JPanel();
        status.setOpaque(false);
        status.setLayout(new BoxLayout(status, BoxLayout.Y_AXIS));
        status.add(points);
        status.add(calibration);
        status.add(ready);
        helpCard.add(status, BorderLayout.SOUTH);
        add(helpCard);
        add(Box.createVerticalStrut(UIScale.scale(12)));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(6), UIScale.scale(4)));
        row.add(undo);
        row.add(previous);
        row.add(skip);
        row.add(next);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height * 2));
        add(row);
        add(Box.createVerticalStrut(UIScale.scale(8)));
        add(finish);
        add(Box.createVerticalStrut(UIScale.scale(16)));
        LoupeView loupe = w.loupe();
        loupe.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(loupe);
        add(Box.createVerticalStrut(UIScale.scale(4)));
        add(loupeHint);
        add(Box.createVerticalStrut(UIScale.scale(8)));
        add(Ui.small(m(Msg.LEGEND)));
        add(Box.createVerticalStrut(UIScale.scale(8)));
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
        progress.setMaximum(window.model().session().steps().size());
        progress.setValue(window.model().session().currentIndex() + 1);
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
