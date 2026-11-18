package com.periomeasure.ui;

import java.util.ArrayList;
import java.util.List;

import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeMeasurements;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.Quantity;
import com.periomeasure.core.Warning;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.i18n.UiTexts;
import com.periomeasure.io.Formats;
import com.periomeasure.io.MeasurementRecord;

/**
 * Texts of the results window, built without any GUI so they can be tested.
 * The key measurements (ICD, PFI with PFH/PFW) come first.
 */
public final class ResultsSummary {

    private static final String MISSING = "–";

    private ResultsSummary() {
    }

    public static String headerLine(MeasurementRecord r, Language l) {
        return Messages.get(l, Msg.RES_HEADER, r.patientId, UiTexts.timing(l, r.timing), r.photoDate.toString());
    }

    /** ICD/IPD line (both eyes only), then one PFI line per eye. */
    public static List<String> keyLines(MeasurementRecord r, Language l) {
        List<String> lines = new ArrayList<>();
        if (r.bilateral != null) {
            lines.add("ICD " + Formats.quantity(r.bilateral.icd, l) + "   ·   IPD "
                    + Formats.quantity(r.bilateral.ipd, l));
        }
        for (Eye eye : r.eyes.eyes()) {
            EyeMeasurements m = r.perEye.get(eye);
            lines.add(eye.name() + "   PFI " + Formats.pfi(m.get(MeasureId.PFI), m.get(MeasureId.PFH),
                    m.get(MeasureId.PFW), l));
        }
        return lines;
    }

    public static String[] columns(MeasurementRecord r, Language l) {
        if (r.bilateral != null) {
            return new String[] {Messages.get(l, Msg.RES_COL_MEASURE), "OD", "OS", Messages.get(l, Msg.RES_COL_DIFF)};
        }
        return new String[] {Messages.get(l, Msg.RES_COL_MEASURE), r.eyes.eyes().get(0).name()};
    }

    /** One row per per-eye measurement that was measured, in priority order. */
    public static List<String[]> tableRows(MeasurementRecord r, Language l) {
        List<String[]> rows = new ArrayList<>();
        for (MeasureId id : MeasureId.values()) {
            if (id.bilateral) {
                continue;
            }
            boolean present = false;
            for (EyeMeasurements m : r.perEye.values()) {
                present |= m.has(id);
            }
            if (!present) {
                continue;
            }
            if (r.bilateral != null) {
                rows.add(new String[] {UiTexts.measureLabel(l, id), q(r.perEye.get(Eye.OD).get(id), l),
                        q(r.perEye.get(Eye.OS).get(id), l), q(r.bilateral.difference(id), l)});
            } else {
                rows.add(new String[] {UiTexts.measureLabel(l, id),
                        q(r.perEye.get(r.eyes.eyes().get(0)).get(id), l)});
            }
        }
        return rows;
    }

    private static String q(Quantity q, Language l) {
        return q == null ? MISSING : Formats.quantity(q, l);
    }

    public static String calibrationLine(MeasurementRecord r, Language l) {
        return Messages.get(l, Msg.RES_CAL, Formats.number(r.calibration.mmPerPx(), 8, l),
                Formats.number(r.calibration.majorAxisPx(), 1, l), Formats.number(r.calibration.axisRatio(), 4, l),
                Formats.number(r.calibration.rmsResidualPx, 2, l), r.calibration.pointCount);
    }

    public static String axisLine(MeasurementRecord r, Language l) {
        return Messages.get(l, Msg.RES_AXIS, UiTexts.axisSource(l, r.axes.source),
                Formats.number(r.axes.tiltDegrees(), 1, l));
    }

    public static List<String> warningLines(MeasurementRecord r, Language l) {
        List<String> out = new ArrayList<>();
        for (Warning w : r.allWarnings()) {
            out.add(UiTexts.warning(l, w));
        }
        return out;
    }

    // ------------------------------------------------------------------ demo verification

    public static String[] verificationColumns(Language l) {
        return new String[] {Messages.get(l, Msg.RES_COL_MEASURE), Messages.get(l, Msg.RES_COL_EYE),
                Messages.get(l, Msg.RES_COL_MEASURED), Messages.get(l, Msg.RES_COL_TRUE),
                Messages.get(l, Msg.RES_COL_ERROR), Messages.get(l, Msg.RES_COL_WITHIN)};
    }

    /** One comparison of a measured quantity with its true value. */
    private static final class Check {
        final MeasureId id;
        final String eye;
        final Quantity measured;
        final double truth;

        Check(MeasureId id, String eye, Quantity measured, double truth) {
            this.id = id;
            this.eye = eye;
            this.measured = measured;
            this.truth = truth;
        }

        double error() {
            return measured.value - truth;
        }

        boolean within() {
            return Math.abs(error()) <= 2 * measured.sigmaTotal() + 1e-12;
        }
    }

    private static List<Check> checks(MeasurementRecord r, com.periomeasure.synthetic.GroundTruth t) {
        List<Check> out = new ArrayList<>();
        for (MeasureId id : MeasureId.values()) {
            if (id.bilateral) {
                if (r.bilateral != null) {
                    out.add(new Check(id, "OD–OS", id == MeasureId.ICD ? r.bilateral.icd : r.bilateral.ipd,
                            id == MeasureId.ICD ? t.icd() : t.ipd()));
                }
                continue;
            }
            for (Eye eye : r.eyes.eyes()) {
                Quantity q = r.perEye.get(eye).get(id);
                Double truth = t.perEye(eye).get(id);
                if (q != null && truth != null) {
                    out.add(new Check(id, eye.name(), q, truth));
                }
            }
        }
        return out;
    }

    /** Rows: measurement, eye, measured ± sigma, true value, error, ✓/✗ (true value within 2 sigma). */
    public static List<String[]> verificationRows(MeasurementRecord r, com.periomeasure.synthetic.GroundTruth t,
            Language l) {
        List<String[]> rows = new ArrayList<>();
        for (Check c : checks(r, t)) {
            int d = Formats.decimals(c.measured.unit) + 1;
            rows.add(new String[] {UiTexts.measureLabel(l, c.id), c.eye, Formats.quantity(c.measured, l),
                    Formats.number(c.truth, d, l) + Formats.unitSuffix(c.measured.unit),
                    Formats.number(c.error(), d, l) + Formats.unitSuffix(c.measured.unit), c.within() ? "✓" : "✗"});
        }
        return rows;
    }

    /** @return the largest absolute error of any length (mm) */
    public static double maxLengthError(MeasurementRecord r, com.periomeasure.synthetic.GroundTruth t) {
        double max = 0;
        for (Check c : checks(r, t)) {
            if (c.measured.unit == com.periomeasure.core.Unit.MM) {
                max = Math.max(max, Math.abs(c.error()));
            }
        }
        return max;
    }

    public static String verificationSummary(MeasurementRecord r, com.periomeasure.synthetic.GroundTruth t,
            Language l) {
        List<Check> all = checks(r, t);
        int ok = 0;
        for (Check c : all) {
            if (c.within()) {
                ok++;
            }
        }
        return Messages.get(l, Msg.RES_VERIFY_SUMMARY, Formats.number(maxLengthError(r, t), 3, l), ok, all.size());
    }

    /** Tab-separated table (pastes into Excel as cells). */
    public static String clipboardText(MeasurementRecord r, Language l) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join("\t", columns(r, l))).append('\n');
        if (r.bilateral != null) {
            sb.append(UiTexts.measureLabel(l, MeasureId.ICD)).append('\t')
                    .append(Formats.quantity(r.bilateral.icd, l)).append('\n');
            sb.append(UiTexts.measureLabel(l, MeasureId.IPD)).append('\t')
                    .append(Formats.quantity(r.bilateral.ipd, l)).append('\n');
        }
        for (String[] row : tableRows(r, l)) {
            sb.append(String.join("\t", row)).append('\n');
        }
        return sb.toString();
    }
}
