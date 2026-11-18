package com.periomeasure.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.io.MeasurementRecord;
import com.periomeasure.synthetic.GroundTruth;

/** Results card: key measurements first, table, (demo) verification, calibration, warnings, saved files. */
final class ResultsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    ResultsPanel(MainWindow w, AppModel.Outcome o) {
        Language l = w.model().settings().language;
        MeasurementRecord r = o.record;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        int pad = UIScale.scale(18);
        setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        add(Ui.title(ResultsSummary.headerLine(r, l), 3));
        add(Box.createVerticalStrut(UIScale.scale(10)));
        JPanel key = Ui.card(null);
        key.setLayout(new BoxLayout(key, BoxLayout.Y_AXIS));
        for (String line : ResultsSummary.keyLines(r, l)) {
            JLabel k = new JLabel(line);
            k.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.round(Themes.font(1))));
            k.setAlignmentX(Component.LEFT_ALIGNMENT);
            key.add(k);
            key.add(Box.createVerticalStrut(UIScale.scale(4)));
        }
        add(key);
        add(Box.createVerticalStrut(UIScale.scale(12)));
        add(table(ResultsSummary.tableRows(r, l), ResultsSummary.columns(r, l), 210, 125, 125, 125));

        GroundTruth truth = w.model().truth();
        if (truth != null) {
            add(Box.createVerticalStrut(UIScale.scale(14)));
            add(Ui.title(Messages.get(l, Msg.RES_VERIFY_TITLE), 1));
            JTextArea s = Ui.text(ResultsSummary.verificationSummary(r, truth, l));
            s.setForeground(Themes.accent());
            add(s);
            add(table(ResultsSummary.verificationRows(r, truth, l), ResultsSummary.verificationColumns(l), 190, 55,
                    130, 95, 90, 80));
        }

        add(Box.createVerticalStrut(UIScale.scale(12)));
        add(Ui.small(ResultsSummary.calibrationLine(r, l) + "\n" + ResultsSummary.axisLine(r, l)));
        List<String> warnings = ResultsSummary.warningLines(r, l);
        StringBuilder sb = new StringBuilder(Messages.get(l, Msg.RES_WARNINGS)).append(":");
        if (warnings.isEmpty()) {
            sb.append(' ').append(Messages.get(l, Msg.RES_NO_WARNINGS));
        }
        for (String s : warnings) {
            sb.append("\n• ").append(s);
        }
        JTextArea wt = Ui.text(sb.toString());
        if (!warnings.isEmpty()) {
            wt.setForeground(Themes.warning());
        }
        add(Box.createVerticalStrut(UIScale.scale(6)));
        add(wt);
        add(Box.createVerticalStrut(UIScale.scale(6)));
        add(Ui.small(Messages.get(l, Msg.RES_SAVED, o.files.sessionFile.getAbsolutePath()) + "\n"
                + Messages.get(l, Msg.RES_SAVED, o.files.cumulativeFile.getAbsolutePath()) + "\n"
                + Messages.get(l, Msg.RES_DISCLAIMER)));

        JButton copy = Ui.primary(Messages.get(l, Msg.BTN_COPY), Icons.Kind.COPY, e -> Toolkit.getDefaultToolkit()
                .getSystemClipboard().setContents(new StringSelection(ResultsSummary.clipboardText(r, l)), null));
        File dir = o.files.sessionFile.getAbsoluteFile().getParentFile();
        JButton open = Ui.button(Messages.get(l, Msg.BTN_OPEN_FOLDER), Icons.of(Icons.Kind.FOLDER), null);
        open.setEnabled(Desktop.isDesktopSupported());
        open.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(dir);
            } catch (Exception ex) {
                open.setEnabled(false);
            }
        });
        JButton again = Ui.button(Messages.get(l, Msg.BTN_NEW_MEASUREMENT), Icons.of(Icons.Kind.REFRESH), e -> {
            w.model().newMeasurement();
            w.refresh();
        });
        JButton other = Ui.button(Messages.get(l, Msg.BTN_OPEN_OTHER), Icons.of(Icons.Kind.OPEN), e -> w.openPhoto());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, UIScale.scale(6), UIScale.scale(6)));
        buttons.add(copy);
        buttons.add(open);
        buttons.add(again);
        buttons.add(other);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(Box.createVerticalStrut(UIScale.scale(8)));
        add(buttons);
        add(Box.createVerticalGlue());
    }

    private static JScrollPane table(List<String[]> rows, String[] cols, int... widths) {
        DefaultTableModel model = new DefaultTableModel(rows.toArray(new Object[0][]), cols) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable t = new JTable(model);
        t.setRowHeight(UIScale.scale(28));
        int total = 0;
        for (int i = 0; i < widths.length && i < cols.length; i++) {
            int wd = UIScale.scale(widths[i]);
            t.getColumnModel().getColumn(i).setPreferredWidth(wd);
            total += wd;
        }
        JScrollPane s = new JScrollPane(t);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Tall enough for every row: no hidden last row, no inner scroll bar.
        int h = t.getTableHeader().getPreferredSize().height + t.getRowHeight() * rows.size() + UIScale.scale(6);
        s.setPreferredSize(new Dimension(Math.max(total, UIScale.scale(540)), h));
        s.setMinimumSize(new Dimension(UIScale.scale(200), h));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        return s;
    }
}
