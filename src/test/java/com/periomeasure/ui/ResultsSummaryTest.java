package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Eye;
import com.periomeasure.core.MeasureId;
import com.periomeasure.i18n.Language;
import com.periomeasure.io.Formats;
import com.periomeasure.io.MeasurementRecord;
import com.periomeasure.io.ResultCsvTestAccess;
import com.periomeasure.synthetic.FaceModel;
import com.periomeasure.synthetic.GroundTruth;

class ResultsSummaryTest {

    private final MeasurementRecord both = ResultCsvTestAccess.bothEyes("P017");
    private final MeasurementRecord od = ResultCsvTestAccess.rightEyeOnly();

    @Test
    void keyLinesPutIcdAndPfiFirst() {
        List<String> lines = ResultsSummary.keyLines(both, Language.TR);
        assertTrue(lines.get(0).startsWith("ICD "), lines.get(0));
        assertTrue(lines.get(0).contains("IPD "), lines.get(0));
        String pfiOd = Formats.pfi(both.perEye.get(Eye.OD).get(MeasureId.PFI), both.perEye.get(Eye.OD).get(MeasureId.PFH),
                both.perEye.get(Eye.OD).get(MeasureId.PFW), Language.TR);
        assertEquals("OD   PFI " + pfiOd, lines.get(1));
        assertTrue(lines.get(2).startsWith("OS   PFI "));
    }

    @Test
    void singleEyeHasNoIcdLine() {
        List<String> lines = ResultsSummary.keyLines(od, Language.EN);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("OD   PFI "));
    }

    @Test
    void tableHasOneRowPerPerEyeMeasurementInPriorityOrder() {
        List<String[]> rows = ResultsSummary.tableRows(both, Language.TR);
        assertEquals(4, ResultsSummary.columns(both, Language.TR).length);
        assertTrue(rows.get(0)[0].startsWith("PFI"));
        assertTrue(rows.get(1)[0].startsWith("PFW"));
        assertTrue(rows.get(2)[0].startsWith("PFH"));
        for (String[] r : rows) {
            assertEquals(4, r.length);
            assertTrue(r[1].contains("±") && r[2].contains("±") && r[3].contains("±"));
        }
        assertEquals(2, ResultsSummary.columns(od, Language.TR).length);
    }

    @Test
    void calibrationAxisAndWarningsAreTranslated() {
        assertTrue(ResultsSummary.calibrationLine(both, Language.TR).startsWith("Kalibrasyon: 0,05000000 mm/px"),
                ResultsSummary.calibrationLine(both, Language.TR));
        assertTrue(ResultsSummary.axisLine(both, Language.TR).contains("pupiller"));
        List<String> w = ResultsSummary.warningLines(both, Language.TR);
        assertTrue(w.stream().anyMatch(s -> s.contains("Görüntü genişliği 1800 px")), w.toString());
        assertTrue(w.stream().anyMatch(s -> s.startsWith("Eğik çekim")), w.toString());
    }

    @Test
    void verificationRowsCompareEveryMeasurementWithGroundTruth() {
        GroundTruth truth = GroundTruth.of(FaceModel.typical()); // same face as the fixture record
        List<String[]> rows = ResultsSummary.verificationRows(both, truth, Language.TR);
        assertEquals(6, ResultsSummary.verificationColumns(Language.TR).length);
        assertTrue(rows.get(0)[0].startsWith("PFI") && "OD".equals(rows.get(0)[1]), rows.get(0)[0]);
        assertTrue(rows.stream().anyMatch(r -> r[0].startsWith("ICD")));
        for (String[] r : rows) {
            assertEquals(6, r.length);
            assertEquals("✓", r[5], String.join(" | ", r));
        }
        assertTrue(ResultsSummary.maxLengthError(both, truth) < 1e-6);
        assertTrue(ResultsSummary.verificationSummary(both, truth, Language.TR).contains(rows.size() + "/" + rows.size()),
                ResultsSummary.verificationSummary(both, truth, Language.TR));
    }

    @Test
    void verificationFlagsAnErrorOutsideTwoSigma() {
        GroundTruth wrongFace = GroundTruth.of(FaceModel.rightPtosis()); // OD MRD1 differs by 5 mm
        List<String[]> rows = ResultsSummary.verificationRows(both, wrongFace, Language.EN);
        assertTrue(rows.stream().anyMatch(r -> r[0].startsWith("MRD1") && "OD".equals(r[1]) && "✗".equals(r[5])));
    }

    @Test
    void clipboardTextIsTabSeparatedForSpreadsheets() {
        String c = ResultsSummary.clipboardText(both, Language.EN);
        String[] lines = c.split("\n");
        assertTrue(lines[0].startsWith("Measurement\tOD\tOS\tOD − OS"), lines[0]);
        assertTrue(c.contains("ICD (intercanthal distance)\t"));
        assertTrue(lines[1].split("\t").length >= 2);
    }
}
