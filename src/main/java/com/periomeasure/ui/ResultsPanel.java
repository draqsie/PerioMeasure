package com.periomeasure.ui;

import java.awt.Color;
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
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        add(Ui.title(ResultsSummary.headerLine(r, l), 14f));
        add(Box.createVerticalStrut(6));
        for (String line : ResultsSummary.keyLines(r, l)) {
            JLabel k = new JLabel(line);
            k.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            k.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(k);
        }
        add(Box.createVerticalStrut(8));
        add(table(ResultsSummary.tableRows(r, l), ResultsSummary.columns(r, l), 200, 115, 115, 115));

        GroundTruth truth = w.model().truth();
        if (truth != null) {
            add(Box.createVerticalStrut(10));
            add(Ui.title(Messages.get(l, Msg.RES_VERIFY_TITLE), 13f));
            JTextArea s = Ui.text(ResultsSummary.verificationSummary(r, truth, l));
            s.setForeground(new Color(0, 90, 160));
            add(s);
            add(table(ResultsSummary.verificationRows(r, truth, l), ResultsSummary.verificationColumns(l), 175, 45,
                    120, 85, 80, 40));
        }

        add(Box.createVerticalStrut(8));
        add(Ui.text(ResultsSummary.calibrationLine(r, l)));
        add(Ui.text(ResultsSummary.axisLine(r, l)));
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
            wt.setForeground(Ui.WARNING);
        }
        add(wt);
        add(Ui.text(Messages.get(l, Msg.RES_SAVED, o.files.sessionFile.getAbsolutePath()) + "\n"
                + Messages.get(l, Msg.RES_SAVED, o.files.cumulativeFile.getAbsolutePath())));
        JLabel disclaimer = new JLabel(Messages.get(l, Msg.RES_DISCLAIMER));
        disclaimer.setFont(disclaimer.getFont().deriveFont(Font.ITALIC));
        disclaimer.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(disclaimer);

        JButton copy = Ui.button(Messages.get(l, Msg.BTN_COPY), e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(ResultsSummary.clipboardText(r, l)), null));
        File dir = o.files.sessionFile.getAbsoluteFile().getParentFile();
        JButton open = Ui.button(Messages.get(l, Msg.BTN_OPEN_FOLDER), null);
        open.setEnabled(Desktop.isDesktopSupported());
        open.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(dir);
            } catch (Exception ex) {
                open.setEnabled(false);
            }
        });
        JButton again = Ui.button(Messages.get(l, Msg.BTN_NEW_MEASUREMENT), e -> {
            w.model().newMeasurement();
            w.refresh();
        });
        JButton other = Ui.button(Messages.get(l, Msg.BTN_OPEN_OTHER), e -> w.openPhoto());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        buttons.add(copy);
        buttons.add(open);
        buttons.add(again);
        buttons.add(other);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(Box.createVerticalStrut(6));
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
        t.setRowHeight(22);
        int total = 0;
        for (int i = 0; i < widths.length && i < cols.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            total += widths[i];
        }
        JScrollPane s = new JScrollPane(t);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Tall enough for every row: no hidden last row, no inner scroll bar.
        int h = t.getTableHeader().getPreferredSize().height + t.getRowHeight() * rows.size() + 6;
        s.setPreferredSize(new Dimension(Math.max(total, 540), h));
        s.setMinimumSize(new Dimension(200, h));
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        return s;
    }
}
