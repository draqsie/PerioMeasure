package com.periomeasure.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.io.PrefsStore;
import com.periomeasure.io.Settings;

import ij.Prefs;

/** Settings window (sticker diameter, output folder, CSV decimal separator, loupe magnification). */
final class SettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private transient Settings result;

    private SettingsDialog(Window owner, Settings current) {
        super(owner, Messages.get(current.language, Msg.SET_TITLE), ModalityType.APPLICATION_MODAL);
        Language l = current.language;
        JSpinner diameter = new JSpinner(new SpinnerNumberModel(current.referenceDiameterMm, Settings.MIN_DIAMETER_MM,
                Settings.MAX_DIAMETER_MM, 0.1));
        JComboBox<String> decimal = new JComboBox<>(new String[] {Messages.get(l, Msg.DEC_DOT),
            Messages.get(l, Msg.DEC_COMMA)});
        decimal.setSelectedIndex(current.decimalSeparator == ',' ? 1 : 0);
        JTextField output = new JTextField(current.outputDir, 28);
        JButton browse = Ui.button(Messages.get(l, Msg.SET_BROWSE), Icons.of(Icons.Kind.FOLDER), e -> {
            JFileChooser ch = new JFileChooser(output.getText().trim().isEmpty() ? null : output.getText().trim());
            ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            ch.setPreferredSize(new Dimension(UIScale.scale(820), UIScale.scale(560)));
            if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                output.setText(ch.getSelectedFile().getAbsolutePath());
            }
        });
        JSlider zoom = new JSlider(Settings.MIN_ZOOM, Settings.MAX_ZOOM, current.zoomFactor);
        zoom.setMajorTickSpacing(1);
        zoom.setPaintTicks(true);
        zoom.setSnapToTicks(true);
        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        for (int z = Settings.MIN_ZOOM; z <= Settings.MAX_ZOOM; z++) {
            labels.put(z, new JLabel(z + "×"));
        }
        zoom.setLabelTable(labels);
        zoom.setPaintLabels(true);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        row = addRow(form, row, Messages.get(l, Msg.SET_DIAMETER), diameter);
        row = addRow(form, row, Messages.get(l, Msg.SET_DECIMAL), decimal);
        JPanel out = new JPanel(new BorderLayout(UIScale.scale(6), 0));
        out.add(output, BorderLayout.CENTER);
        out.add(browse, BorderLayout.EAST);
        row = addRow(form, row, Messages.get(l, Msg.SET_OUTPUT), out);
        addRow(form, row, Messages.get(l, Msg.SET_ZOOM), zoom);

        JTextArea error = Ui.text(" ");
        error.setForeground(Themes.error());
        JButton save = Ui.primary(Messages.get(l, Msg.SET_SAVE), Icons.Kind.CHECK, e -> {
            try {
                Settings s = current.withReferenceDiameterMm(((Number) diameter.getValue()).doubleValue())
                        .withDecimalSeparator(decimal.getSelectedIndex() == 1 ? ',' : '.')
                        .withOutputDir(output.getText()).withZoomFactor(zoom.getValue());
                PrefsStore store = new ImageJPrefsStore();
                s.save(store);
                Prefs.savePreferences();
                result = s;
                dispose();
            } catch (IllegalArgumentException ex) {
                error.setText(Messages.get(l, Msg.ERR_DIAMETER));
            }
        });
        JButton cancel = Ui.button(Messages.get(l, Msg.BTN_CANCEL), e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(8), 0));
        buttons.add(cancel);
        buttons.add(save);
        JPanel south = new JPanel(new BorderLayout());
        south.add(error, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        int pad = UIScale.scale(18);
        JPanel root = new JPanel(new BorderLayout(0, UIScale.scale(12)));
        root.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        root.add(form, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(save);
        pack();
        setLocationRelativeTo(owner);
    }

    private static int addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(UIScale.scale(6), 0, UIScale.scale(6), UIScale.scale(12));
        c.anchor = GridBagConstraints.WEST;
        JLabel l = new JLabel(label);
        form.add(l, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(UIScale.scale(6), 0, UIScale.scale(6), 0);
        form.add(field, c);
        return row + 1;
    }

    /** @return the new settings (already saved), or null if cancelled */
    static Settings show(Window owner, Settings current) {
        SettingsDialog d = new SettingsDialog(owner, current);
        d.setVisible(true);
        return d.result;
    }
}
