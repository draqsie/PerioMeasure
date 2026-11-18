package com.periomeasure.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.Version;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.io.Theme;

/**
 * Top bar: app name, primary actions on the left, view controls in the
 * middle, language / theme / settings / guide / about on the right. The
 * public fields are the targets the guided tour points at.
 */
final class HeaderBar extends JPanel {

    private static final long serialVersionUID = 1L;

    final JButton open;
    final JButton paste;
    final JButton demo;
    final JPanel viewGroup;
    final JComboBox<String> language;
    final JButton theme;
    final JButton settings;
    final JButton help;
    final JButton about;

    HeaderBar(MainWindow w) {
        super(new BorderLayout());
        Language l = w.model().settings().language;
        int gap = UIScale.scale(8);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")),
                BorderFactory.createEmptyBorder(gap, UIScale.scale(14), gap, UIScale.scale(10))));

        JLabel name = new JLabel("PerioMeasure");
        name.setFont(name.getFont().deriveFont(Font.BOLD, Themes.font(5)));
        name.setForeground(Themes.accent());
        name.setToolTipText("PerioMeasure " + Version.get());
        open = Ui.primary(Messages.get(l, Msg.TB_OPEN), Icons.Kind.OPEN, e -> w.openPhoto());
        demo = Ui.button(Messages.get(l, Msg.TB_DEMO) + "  ▾", Icons.of(Icons.Kind.DEMO), null);
        demo.addActionListener(e -> w.demoMenu().show(demo, 0, demo.getHeight()));
        paste = Ui.button(Messages.get(l, Msg.TB_PASTE), Icons.of(Icons.Kind.PASTE), e -> w.paste());
        paste.setToolTipText(Messages.get(l, Msg.TB_PASTE_TIP));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        left.add(name);
        left.add(Box.createHorizontalStrut(UIScale.scale(14)));
        left.add(open);
        left.add(paste);
        left.add(demo);

        viewGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, UIScale.scale(2), 0));
        viewGroup.add(Ui.flat(Messages.get(l, Msg.TB_FIT), Icons.Kind.FIT, Messages.get(l, Msg.TB_FIT),
                e -> w.viewer().fit()));
        viewGroup.add(Ui.flat(null, Icons.Kind.ZOOM_OUT, Messages.get(l, Msg.TB_ZOOM_OUT),
                e -> w.viewer().zoom(1 / 1.5)));
        viewGroup.add(Ui.flat(null, Icons.Kind.ZOOM_IN, Messages.get(l, Msg.TB_ZOOM_IN),
                e -> w.viewer().zoom(1.5)));
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        center.add(viewGroup);

        language = new JComboBox<>(new String[] {"English", "Türkçe"});
        language.setSelectedIndex(l == Language.EN ? 0 : 1);
        language.setToolTipText(Messages.get(l, Msg.TB_LANGUAGE));
        language.addActionListener(e -> w.setLanguage(language.getSelectedIndex() == 0 ? Language.EN : Language.TR));
        JLabel globe = new JLabel(Icons.of(Icons.Kind.GLOBE));
        globe.setToolTipText(Messages.get(l, Msg.TB_LANGUAGE));
        boolean dark = w.model().settings().theme == Theme.DARK;
        theme = Ui.flat(Messages.get(l, dark ? Msg.TB_THEME_LIGHT : Msg.TB_THEME_DARK),
                dark ? Icons.Kind.SUN : Icons.Kind.MOON, null, e -> w.toggleTheme());
        settings = Ui.flat(Messages.get(l, Msg.TB_SETTINGS), Icons.Kind.SETTINGS, null, e -> w.openSettings());
        help = Ui.flat(Messages.get(l, Msg.TB_HELP), Icons.Kind.HELP, null, e -> w.startTour());
        about = Ui.flat(null, Icons.Kind.INFO, Messages.get(l, Msg.TB_ABOUT), e -> w.showAbout());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(4), 0));
        right.add(globe);
        right.add(language);
        right.add(separator());
        right.add(theme);
        right.add(settings);
        right.add(help);
        right.add(about);

        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    private static JSeparator separator() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new java.awt.Dimension(UIScale.scale(1), UIScale.scale(24)));
        return s;
    }
}
