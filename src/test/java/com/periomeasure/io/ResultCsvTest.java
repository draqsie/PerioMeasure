package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.periomeasure.core.BilateralMeasurements;
import com.periomeasure.core.Calibration;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeMeasurements;
import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.MeasurementEngine;
import com.periomeasure.core.ReferenceAxes;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;
import com.periomeasure.synthetic.FaceModel;
import com.periomeasure.synthetic.SyntheticScene;

class ResultCsvTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 9, 30, 5);
    private File dir;

    static MeasurementRecord bothEyes(String patientId) {
        SyntheticScene scene = SyntheticScene.of(FaceModel.typical(), 0.05, 3, false, 0.95);
        Calibration cal = Calibration.fromEdgePoints(scene.stickerEdgePoints(20, 0, 0), Calibration.Method.MANUAL, 24);
        BilateralMeasurements b = MeasurementEngine.measureBoth(scene.landmarksPx(Eye.OD), scene.landmarksPx(Eye.OS),
                cal.scale());
        Map<Eye, EyeMeasurements> per = new EnumMap<>(Eye.class);
        per.put(Eye.OD, b.od);
        per.put(Eye.OS, b.os);
        return new MeasurementRecord("0.2.0", NOW, patientId, Timing.PRE_OP, LocalDate.of(2026, 3, 2),
                FileHash.sha256Hex("P017_pre.jpg"), scene.width, scene.height, cal, b.axes, EyeSelection.BOTH, per, b,
                Collections.singletonList(new Warning(WarningCode.IMAGE_TOO_NARROW, null, 1800)));
    }

    static MeasurementRecord rightEyeOnly() {
        SyntheticScene scene = SyntheticScene.of(FaceModel.typical(), 0.05, 0, false, 1.0);
        Calibration cal = Calibration.fromEdgePoints(scene.stickerEdgePoints(20, 0, 0), Calibration.Method.MANUAL, 24);
        ReferenceAxes ax = ReferenceAxes.imageHorizontal();
        EyeMeasurements od = MeasurementEngine.measureEye(Eye.OD, scene.landmarksPx(Eye.OD), ax, cal.scale());
        Map<Eye, EyeMeasurements> per = new EnumMap<>(Eye.class);
        per.put(Eye.OD, od);
        return new MeasurementRecord("0.2.0", NOW, "P018", Timing.POST_OP, LocalDate.of(2026, 6, 1), "",
                scene.width, scene.height, cal, ax, EyeSelection.OD, per, null, Collections.<Warning>emptyList());
    }

    private static int col(String name) {
        int i = ResultCsv.header().indexOf(name);
        assertTrue(i >= 0, "missing column " + name);
        return i;
    }

    @BeforeEach
    void cleanDir() throws Exception {
        dir = new File("target/test-output/csv/" + System.nanoTime());
        assertTrue(dir.mkdirs());
    }

    @Test
    void headerStartsWithIdentifiersAndListsEveryMeasurementWithUncertainty() {
        List<String> h = ResultCsv.header();
        assertEquals(Arrays.asList("plugin_version", "measured_at", "patient_id", "eye", "pre_post", "photo_date",
                "image_file_sha256"), h.subList(0, 7));
        for (MeasureId id : MeasureId.values()) {
            col(id.name());
            col(id.name() + "_sigma");
            col(id.name() + "_sigma_click");
            col(id.name() + "_sigma_cal");
        }
        assertTrue(col("PFI") < col("ICD") && col("ICD") < col("PFW") && col("PFW") < col("PFH"));
        assertEquals("warnings", h.get(h.size() - 1));
        for (String c : Arrays.asList("calibration_method", "reference_diameter_mm", "sticker_major_axis_px",
                "sticker_minor_axis_px", "sticker_axis_ratio", "sticker_rms_px", "sticker_points", "mm_per_px",
                "mm_per_px_rel_sigma", "axis_source", "axis_tilt_deg", "image_width_px", "image_height_px")) {
            col(c);
        }
    }

    @Test
    void bothEyesGiveOdOsAndDifferenceRowsWithDotDecimals() {
        MeasurementRecord r = bothEyes("P017");
        List<List<String>> rows = ResultCsv.rows(r, '.');
        assertEquals(3, rows.size());
        assertEquals("OD", rows.get(0).get(col("eye")));
        assertEquals("OS", rows.get(1).get(col("eye")));
        assertEquals("OD-OS", rows.get(2).get(col("eye")));
        assertEquals("PRE", rows.get(0).get(col("pre_post")));
        assertEquals("2026-03-02", rows.get(0).get(col("photo_date")));
        assertEquals("2026-11-18T09:30:05", rows.get(0).get(col("measured_at")));
        assertEquals("MANUAL", rows.get(0).get(col("calibration_method")));
        assertEquals("PUPILS", rows.get(0).get(col("axis_source")));

        double pfi = Double.parseDouble(rows.get(0).get(col("PFI")));
        assertEquals(r.perEye.get(Eye.OD).get(MeasureId.PFI).value, pfi, 1e-5);
        double pfw = Double.parseDouble(rows.get(1).get(col("PFW")));
        assertEquals(r.perEye.get(Eye.OS).get(MeasureId.PFW).value, pfw, 1e-4);
        double icd = Double.parseDouble(rows.get(0).get(col("ICD")));
        assertEquals(r.bilateral.icd.value, icd, 1e-4);
        assertEquals(rows.get(0).get(col("ICD")), rows.get(1).get(col("ICD")));
        assertEquals("", rows.get(2).get(col("ICD")));
        double diff = Double.parseDouble(rows.get(2).get(col("PFW")));
        assertEquals(r.bilateral.difference(MeasureId.PFW).value, diff, 1e-4);
        double mmPerPx = Double.parseDouble(rows.get(0).get(col("mm_per_px")));
        assertEquals(0.05, mmPerPx, 1e-8);
        assertTrue(rows.get(0).get(col("warnings")).contains("IMAGE_TOO_NARROW"));
        assertTrue(rows.get(0).get(col("warnings")).contains("OBLIQUE_VIEW"));
    }

    @Test
    void commaDecimalUsesSemicolonDelimiter() {
        MeasurementRecord r = bothEyes("P017");
        assertEquals(';', ResultCsv.delimiterFor(','));
        assertEquals(',', ResultCsv.delimiterFor('.'));
        List<String> row = ResultCsv.rows(r, ',').get(0);
        String pfi = row.get(col("PFI"));
        assertTrue(pfi.matches("\\d+,\\d{5}"), pfi);
        assertEquals(r.perEye.get(Eye.OD).get(MeasureId.PFI).value, Double.parseDouble(pfi.replace(',', '.')), 1e-5);
    }

    @Test
    void textFieldsAreQuotedAndProtectedAgainstFormulaInjection() {
        assertEquals("\"a,b\"", ResultCsv.toLine(Collections.singletonList("a,b"), ','));
        assertEquals("\"say \"\"hi\"\"\"", ResultCsv.toLine(Collections.singletonList("say \"hi\""), ','));
        assertEquals("a;b", ResultCsv.toLine(Arrays.asList("a", "b"), ';'));
        assertEquals("'=HYPERLINK(1)", ResultCsv.rows(bothEyes("=HYPERLINK(1)"), '.').get(0).get(col("patient_id")));
    }

    @Test
    void singleEyeRecordHasOneRowAndEmptyBilateralColumns() {
        List<List<String>> rows = ResultCsv.rows(rightEyeOnly(), '.');
        assertEquals(1, rows.size());
        assertEquals("OD", rows.get(0).get(col("eye")));
        assertEquals("POST", rows.get(0).get(col("pre_post")));
        assertEquals("IMAGE_HORIZONTAL", rows.get(0).get(col("axis_source")));
        assertEquals("", rows.get(0).get(col("ICD")));
        assertEquals("", rows.get(0).get(col("IPD")));
        assertNotEquals("", rows.get(0).get(col("PFW")));
        assertEquals("", rows.get(0).get(col("image_file_sha256")));
    }

    @Test
    void writesSessionFileAndAppendsToTheCumulativeFile() throws Exception {
        ResultCsv.Written w1 = ResultCsv.write(bothEyes("P017"), dir, '.');
        ResultCsv.Written w2 = ResultCsv.write(bothEyes("P017"), dir, '.');
        assertEquals(new File(dir, ResultCsv.CUMULATIVE_FILE), w1.cumulativeFile);
        assertEquals(w1.cumulativeFile, w2.cumulativeFile);
        assertTrue(w1.sessionFile.getName().startsWith("PM_P017_PRE_20260302_"), w1.sessionFile.getName());

        byte[] bytes = Files.readAllBytes(w1.sessionFile.toPath());
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(bytes, 3)); // UTF-8 BOM for Excel
        List<String> session = Files.readAllLines(w1.sessionFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(1 + 3, session.size());
        List<String> cumulative = Files.readAllLines(w1.cumulativeFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(1 + 3 + 3, cumulative.size());
        assertEquals(1, cumulative.stream().filter(l -> l.contains("plugin_version")).count());
    }

    @Test
    void cumulativeFileWithAnotherHeaderIsNeverMixed() throws Exception {
        File existing = new File(dir, ResultCsv.CUMULATIVE_FILE);
        Files.write(existing.toPath(), "old,header\n1,2\n".getBytes(StandardCharsets.UTF_8));
        ResultCsv.Written w = ResultCsv.write(bothEyes("P017"), dir, '.');
        assertEquals(new File(dir, "periomeasure_results_2.csv"), w.cumulativeFile);
        assertEquals("old,header\n1,2\n", new String(Files.readAllBytes(existing.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    void decimalSeparatorChangeIsAlsoAHeaderChange() throws Exception {
        ResultCsv.write(bothEyes("P017"), dir, '.');
        ResultCsv.Written w = ResultCsv.write(bothEyes("P017"), dir, ',');
        assertEquals(new File(dir, "periomeasure_results_2.csv"), w.cumulativeFile);
    }

    @Test
    void patientIdIsSanitisedInFileNames() {
        String name = ResultCsv.sessionFileName(bothEyes("Ş/ük ..\\x"));
        assertTrue(name.matches("PM_[A-Za-z0-9_-]+_PRE_20260302_093005\\.csv"), name);
    }
}
