package com.periomeasure.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.EnumSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Warning;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.i18n.UiTexts;
import com.periomeasure.io.Formats;
import com.periomeasure.session.AxisMode;
import com.periomeasure.session.OptionalLandmark;
import com.periomeasure.session.SessionSetup;

/** Form shown after a photo is loaded: patient code, date, eye(s), timing, options. */
final class SetupPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final transient MainWindow window;
    private final transient Language lang;
    private final JTextField patient = new JTextField(14);
    private final JTextField date = new JTextField(10);
    private final JRadioButton both;
    private final JRadioButton od;
    private final JRadioButton os;
    private final JRadioButton pre;
    private final JRadioButton post;
    private final JRadioButton axisImage;
    private final JRadioButton axisLine;
    private final JCheckBox crease;
    private final JCheckBox brow;
    private final JCheckBox limbus;
    private final JCheckBox contour;
    private final JLabel error = new JLabel(" ");
    final JButton start;

    SetupPanel(MainWindow w, String photoName, LocalDate dateTaken) {
        this.window = w;
        this.lang = w.model().settings().language;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        add(Ui.title(m(Msg.SETUP_TITLE), 18f));
        add(Ui.text(Messages.get(lang, Msg.SETUP_PHOTO, photoName, w.model().width(), w.model().height())));
        if (!w.model().imageWarnings().isEmpty()) {
            StringBuilder sb = new StringBuilder(m(Msg.PANEL_IMAGE_WARNINGS));
            for (Warning wn : w.model().imageWarnings()) {
                sb.append("\n• ").append(UiTexts.warning(lang, wn));
            }
            JTextArea t = Ui.text(sb.toString());
            t.setForeground(Ui.WARNING);
            add(t);
        }
        add(Box.createVerticalStrut(10));

        add(label(Msg.SETUP_PATIENT));
        add(field(patient));
        JLabel privacy = new JLabel(m(Msg.SETUP_PRIVACY));
        privacy.setFont(privacy.getFont().deriveFont(Font.ITALIC, 11f));
        privacy.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(privacy);
        add(Box.createVerticalStrut(8));
        add(label(Msg.SETUP_DATE));
        if (dateTaken != null) {
            date.setText(dateTaken.toString());
        }
        add(field(date));
        add(Box.createVerticalStrut(8));

        add(label(Msg.SETUP_EYE));
        both = new JRadioButton(m(Msg.EYES_BOTH), true);
        od = new JRadioButton(m(Msg.EYES_OD));
        os = new JRadioButton(m(Msg.EYES_OS));
        add(group(both, od, os));
        add(label(Msg.SETUP_TIMING));
        pre = new JRadioButton(m(Msg.TIMING_PRE), true);
        post = new JRadioButton(m(Msg.TIMING_POST));
        add(group(pre, post));
        add(label(Msg.SETUP_AXIS));
        axisImage = new JRadioButton(m(Msg.AXIS_IMAGE), true);
        axisLine = new JRadioButton(m(Msg.AXIS_LINE));
        add(group(axisImage, axisLine));
        both.addActionListener(e -> updateAxis());
        od.addActionListener(e -> updateAxis());
        os.addActionListener(e -> updateAxis());
        updateAxis();

        add(Box.createVerticalStrut(6));
        add(label(Msg.SETUP_OPTIONAL));
        crease = check(Msg.OPT_CREASE);
        brow = check(Msg.OPT_BROW);
        limbus = check(Msg.OPT_LIMBUS);
        contour = check(Msg.OPT_CONTOUR);
        add(Box.createVerticalStrut(6));
        add(Ui.text(Messages.get(lang, Msg.SETUP_STICKER,
                Formats.number(w.model().settings().referenceDiameterMm, 1, lang))));
        add(Box.createVerticalStrut(10));
        error.setForeground(new Color(180, 0, 0));
        error.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(error);
        start = Ui.button(m(Msg.SETUP_START), e -> submit());
        start.setFont(start.getFont().deriveFont(Font.BOLD, 15f));
        start.setAlignmentX(Component.LEFT_ALIGNMENT);
        start.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        add(start);
        add(Box.createVerticalGlue());
    }

    private String m(Msg msg) {
        return Messages.get(lang, msg);
    }

    private JLabel label(Msg msg) {
        JLabel l = new JLabel(m(msg));
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JTextField field(JTextField f) {
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, f.getPreferredSize().height));
        return f;
    }

    private static JPanel group(JRadioButton... buttons) {
        ButtonGroup g = new ButtonGroup();
        JPanel p = new JPanel(new GridLayout(0, 1));
        for (JRadioButton b : buttons) {
            g.add(b);
            p.add(b);
        }
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private JCheckBox check(Msg msg) {
        JCheckBox c = new JCheckBox(m(msg));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(c);
        return c;
    }

    private void updateAxis() {
        boolean single = !both.isSelected();
        axisImage.setEnabled(single);
        axisLine.setEnabled(single);
    }

    /** Fills the form (used by the self-test). */
    void fill(String code, LocalDate d) {
        patient.setText(code);
        date.setText(d.toString());
    }

    private void submit() {
        if (patient.getText().trim().isEmpty()) {
            error.setText(m(Msg.ERR_PATIENT));
            return;
        }
        LocalDate d;
        try {
            d = SessionSetup.parseDate(date.getText());
        } catch (IllegalArgumentException e) {
            error.setText(Messages.get(lang, Msg.ERR_DATE, date.getText().trim()));
            return;
        }
        EnumSet<OptionalLandmark> opt = EnumSet.noneOf(OptionalLandmark.class);
        if (crease.isSelected()) {
            opt.add(OptionalLandmark.CREASE);
        }
        if (brow.isSelected()) {
            opt.add(OptionalLandmark.BROW);
        }
        if (limbus.isSelected()) {
            opt.add(OptionalLandmark.LIMBUS);
        }
        if (contour.isSelected()) {
            opt.add(OptionalLandmark.CONTOUR);
        }
        EyeSelection eyes = both.isSelected() ? EyeSelection.BOTH
                : od.isSelected() ? EyeSelection.OD : EyeSelection.OS;
        window.model().start(new SessionSetup(patient.getText(), d, pre.isSelected() ? Timing.PRE_OP : Timing.POST_OP,
                eyes, axisLine.isSelected() ? AxisMode.USER_LINE : AxisMode.IMAGE_HORIZONTAL, opt));
        window.refresh();
    }
}
