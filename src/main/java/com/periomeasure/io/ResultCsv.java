package com.periomeasure.io;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.periomeasure.core.BilateralMeasurements;
import com.periomeasure.core.Calibration;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeMeasurements;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.Quantity;
import com.periomeasure.core.Unit;
import com.periomeasure.core.Warning;

/**
 * CSV export (UTF-8 with BOM so Excel shows Turkish letters, CRLF line ends,
 * RFC 4180 quoting). One row per measured eye, plus an "OD-OS" difference
 * row when both eyes are measured. With a comma decimal separator the field
 * delimiter becomes a semicolon (Excel convention in Turkey/Europe).
 *
 * <p>Each session is written to its own file and also appended to a
 * cumulative file in the same folder. The cumulative file is only appended
 * to if its header is identical; otherwise a new numbered file is started,
 * so rows with different layouts are never mixed.
 */
public final class ResultCsv {

    public static final String CUMULATIVE_FILE = "periomeasure_results.csv";
    static final String EOL = "\r\n";
    private static final byte[] BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter MEASURED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("HHmmss");

    private static final List<String> BASE_COLUMNS = Collections.unmodifiableList(Arrays.asList(
            "plugin_version", "measured_at", "patient_id", "eye", "pre_post", "photo_date", "image_file_sha256",
            "image_width_px", "image_height_px", "calibration_method", "reference_diameter_mm",
            "sticker_major_axis_px", "sticker_minor_axis_px", "sticker_axis_ratio", "sticker_rms_px",
            "sticker_points", "mm_per_px", "mm_per_px_rel_sigma", "axis_source", "axis_tilt_deg"));

    /** Files written by {@link #write}. */
    public static final class Written {
        public final File sessionFile;
        public final File cumulativeFile;

        Written(File sessionFile, File cumulativeFile) {
            this.sessionFile = sessionFile;
            this.cumulativeFile = cumulativeFile;
        }
    }

    private ResultCsv() {
    }

    public static List<String> header() {
        List<String> h = new ArrayList<>(BASE_COLUMNS);
        for (MeasureId id : MeasureId.values()) {
            h.add(id.name());
            h.add(id.name() + "_sigma");
            h.add(id.name() + "_sigma_click");
            h.add(id.name() + "_sigma_cal");
        }
        h.add("warnings");
        return Collections.unmodifiableList(h);
    }

    public static char delimiterFor(char decimalSeparator) {
        return decimalSeparator == ',' ? ';' : ',';
    }

    public static List<List<String>> rows(MeasurementRecord r, char dec) {
        List<List<String>> rows = new ArrayList<>();
        for (Eye eye : r.eyes.eyes()) {
            List<String> row = base(r, eye.name(), dec);
            EyeMeasurements m = r.perEye.get(eye);
            for (MeasureId id : MeasureId.values()) {
                Quantity q;
                if (id.bilateral) {
                    q = r.bilateral == null ? null : id == MeasureId.ICD ? r.bilateral.icd : r.bilateral.ipd;
                } else {
                    q = m.get(id);
                }
                addQuantity(row, q, id.unit, dec);
            }
            row.add(warnings(r, eye, false));
            rows.add(row);
        }
        if (r.bilateral != null) {
            BilateralMeasurements b = r.bilateral;
            List<String> row = base(r, "OD-OS", dec);
            for (MeasureId id : MeasureId.values()) {
                addQuantity(row, id.bilateral ? null : b.difference(id), id.unit, dec);
            }
            row.add(warnings(r, null, true));
            rows.add(row);
        }
        return rows;
    }

    private static List<String> base(MeasurementRecord r, String eyeLabel, char dec) {
        Calibration c = r.calibration;
        List<String> row = new ArrayList<>();
        row.add(text(r.pluginVersion));
        row.add(r.measuredAt.withNano(0).format(MEASURED_AT));
        row.add(text(r.patientId));
        row.add(eyeLabel);
        row.add(r.timing.code);
        row.add(r.photoDate.format(DATE));
        row.add(r.imageFileSha256);
        row.add(Integer.toString(r.imageWidth));
        row.add(Integer.toString(r.imageHeight));
        row.add(c.method.name());
        row.add(num(c.referenceDiameterMm, 3, dec));
        row.add(num(c.majorAxisPx(), 3, dec));
        row.add(num(c.minorAxisPx(), 3, dec));
        row.add(num(c.axisRatio(), 5, dec));
        row.add(num(c.rmsResidualPx, 4, dec));
        row.add(Integer.toString(c.pointCount));
        row.add(num(c.mmPerPx(), 8, dec));
        row.add(num(c.relativeUncertainty(), 8, dec));
        row.add(r.axes.source.name());
        row.add(num(r.axes.tiltDegrees(), 3, dec));
        return row;
    }

    private static void addQuantity(List<String> row, Quantity q, Unit unit, char dec) {
        int d = decimals(unit);
        if (q == null) {
            row.add("");
            row.add("");
            row.add("");
            row.add("");
        } else {
            row.add(num(q.value, d, dec));
            row.add(num(q.sigmaTotal(), d, dec));
            row.add(num(q.sigmaClick, d, dec));
            row.add(num(q.sigmaCal, d, dec));
        }
    }

    /** CSV decimals: rounding error at most 5e-5 (mm, mm^2, degrees) or 5e-6 (ratios). */
    static int decimals(Unit unit) {
        return unit == Unit.RATIO ? 5 : 4;
    }

    private static String warnings(MeasurementRecord r, Eye eye, boolean imageLevelOnly) {
        List<String> parts = new ArrayList<>();
        for (Warning w : r.allWarnings()) {
            boolean include = imageLevelOnly ? w.eye == null : (w.eye == null || w.eye == eye);
            if (include) {
                parts.add(w.toString());
            }
        }
        return String.join(" | ", parts);
    }

    static String num(double v, int decimals, char dec) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "";
        }
        String s = String.format(Locale.ROOT, "%." + decimals + "f", v);
        if (s.startsWith("-") && Double.parseDouble(s) == 0) {
            s = s.substring(1);
        }
        return dec == ',' ? s.replace('.', ',') : s;
    }

    /** Neutralises spreadsheet formula injection in free-text fields. */
    static String text(String s) {
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) {
            return "'" + s;
        }
        return s;
    }

    public static String toLine(List<String> fields, char delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            String f = fields.get(i);
            if (f.indexOf(delimiter) >= 0 || f.indexOf('"') >= 0 || f.indexOf('\n') >= 0 || f.indexOf('\r') >= 0) {
                sb.append('"').append(f.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(f);
            }
        }
        return sb.toString();
    }

    public static String sessionFileName(MeasurementRecord r) {
        String id = r.patientId.replaceAll("[^A-Za-z0-9_-]", "_");
        if (id.isEmpty()) {
            id = "patient";
        }
        if (id.length() > 40) {
            id = id.substring(0, 40);
        }
        return "PM_" + id + "_" + r.timing.code + "_" + r.photoDate.format(FILE_DATE) + "_"
                + r.measuredAt.format(FILE_TIME) + ".csv";
    }

    /**
     * Writes the session file and appends to the cumulative file.
     *
     * @throws IOException if the folder cannot be created or a file cannot be written
     */
    public static Written write(MeasurementRecord r, File dir, char dec) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create folder " + dir.getAbsolutePath());
        }
        char delim = delimiterFor(dec);
        String headerLine = toLine(header(), delim);
        StringBuilder body = new StringBuilder();
        for (List<String> row : rows(r, dec)) {
            body.append(toLine(row, delim)).append(EOL);
        }
        byte[] rowBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        byte[] headerBytes = (headerLine + EOL).getBytes(StandardCharsets.UTF_8);

        File session = unique(dir, sessionFileName(r));
        writeNew(session, headerBytes, rowBytes);

        File cumulative = null;
        for (int n = 1; cumulative == null; n++) {
            File f = new File(dir, n == 1 ? CUMULATIVE_FILE : "periomeasure_results_" + n + ".csv");
            if (!f.exists()) {
                writeNew(f, headerBytes, rowBytes);
                cumulative = f;
            } else if (headerLine.equals(firstLine(f))) {
                Files.write(f.toPath(), rowBytes, StandardOpenOption.APPEND);
                cumulative = f;
            }
        }
        return new Written(session, cumulative);
    }

    private static void writeNew(File f, byte[] header, byte[] rows) throws IOException {
        byte[] all = new byte[BOM.length + header.length + rows.length];
        System.arraycopy(BOM, 0, all, 0, BOM.length);
        System.arraycopy(header, 0, all, BOM.length, header.length);
        System.arraycopy(rows, 0, all, BOM.length + header.length, rows.length);
        Files.write(f.toPath(), all, StandardOpenOption.CREATE_NEW);
    }

    private static File unique(File dir, String name) {
        File f = new File(dir, name);
        String stem = name.substring(0, name.length() - 4);
        for (int n = 2; f.exists(); n++) {
            f = new File(dir, stem + "_" + n + ".csv");
        }
        return f;
    }

    private static String firstLine(File f) throws IOException {
        String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        if (s.startsWith("﻿")) {
            s = s.substring(1);
        }
        int end = s.length();
        int cr = s.indexOf('\r');
        int lf = s.indexOf('\n');
        if (cr >= 0) {
            end = Math.min(end, cr);
        }
        if (lf >= 0) {
            end = Math.min(end, lf);
        }
        return s.substring(0, end);
    }
}
