package com.periomeasure.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.BilateralMeasurements;
import com.periomeasure.core.Calibration;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeMeasurements;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.MeasurementEngine;
import com.periomeasure.core.Unit;
import com.periomeasure.core.Vec2;
import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;

/**
 * End-to-end: a face with known millimetre geometry is placed in an image at
 * a known scale, rotation, sticker tilt and mirroring; the plugin pipeline
 * (calibration -> measurement) must recover the millimetre values computed
 * independently by {@link GroundTruth}.
 */
class EndToEndTest {

    /** Project acceptance criterion (rules.md): end-to-end error below 0.05 mm. */
    private static final double ACCEPTANCE_MM = 0.05;

    private static boolean has(List<Warning> ws, WarningCode code) {
        for (Warning w : ws) {
            if (w.code == code) {
                return true;
            }
        }
        return false;
    }

    private static double tolerance(Unit unit) {
        switch (unit) {
            case DEGREE:
                return 1e-6;
            case RATIO:
                return 1e-9;
            default:
                return 1e-6; // far below ACCEPTANCE_MM; exact inputs must give (numerically) exact output
        }
    }

    @Test
    void exactInputsRecoverGroundTruthAcrossScalesRotationsTiltsAndMirroring() {
        FaceModel[] faces = {FaceModel.typical(), FaceModel.rightPtosis(), FaceModel.asymmetric()};
        double[] scales = {0.02, 0.04, 0.08};
        double[] rolls = {0, 7.5, -15};
        double[] ratios = {1.0, 0.95, 0.85};
        int cases = 0;
        double worstMm = 0;
        for (FaceModel face : faces) {
            GroundTruth truth = GroundTruth.of(face);
            for (double mmPerPx : scales) {
                for (double roll : rolls) {
                    for (double ratio : ratios) {
                        for (boolean mirrored : new boolean[] {false, true}) {
                            SyntheticScene scene = SyntheticScene.of(face, mmPerPx, roll, mirrored, ratio);
                            Calibration cal = Calibration.fromEdgePoints(scene.stickerEdgePoints(24, 0, 0),
                                    Calibration.Method.MANUAL, face.stickerDiameterMm);
                            assertEquals(mmPerPx, cal.mmPerPx(), 1e-12 * mmPerPx);
                            BilateralMeasurements m = MeasurementEngine.measureBoth(scene.landmarksPx(Eye.OD),
                                    scene.landmarksPx(Eye.OS), cal.scale());
                            String where = face.name + " s=" + mmPerPx + " roll=" + roll + " ratio=" + ratio
                                    + " mirrored=" + mirrored;
                            for (Eye eye : Eye.values()) {
                                EyeMeasurements em = eye == Eye.OD ? m.od : m.os;
                                for (Map.Entry<MeasureId, Double> e : truth.perEye(eye).entrySet()) {
                                    double got = em.get(e.getKey()).value;
                                    double err = Math.abs(got - e.getValue());
                                    assertTrue(err < tolerance(e.getKey().unit),
                                            where + " " + eye + " " + e.getKey() + " expected " + e.getValue()
                                                    + " got " + got);
                                    if (e.getKey().unit == Unit.MM) {
                                        worstMm = Math.max(worstMm, err);
                                    }
                                }
                            }
                            assertEquals(truth.icd(), m.icd.value, 1e-6, where + " ICD");
                            assertEquals(truth.ipd(), m.ipd.value, 1e-6, where + " IPD");
                            assertEquals(mirrored, has(m.warnings(), WarningCode.IMAGE_MIRRORED), where);
                            assertEquals(ratio < 0.97, has(cal.warnings(), WarningCode.OBLIQUE_VIEW), where);
                            cases++;
                        }
                    }
                }
            }
        }
        assertEquals(162, cases);
        assertTrue(worstMm < ACCEPTANCE_MM);
    }

    /**
     * With noisy sticker-edge clicks the scale error must be covered by the
     * reported calibration uncertainty: |error| <= 2 sigma_cal in ~95 % of trials.
     */
    @Test
    void noisyCalibrationErrorIsCoveredByReportedUncertainty() {
        FaceModel face = FaceModel.typical();
        GroundTruth truth = GroundTruth.of(face);
        SyntheticScene scene = SyntheticScene.of(face, 0.04, 5, false, 0.98);
        Random seeds = new Random(77);
        int trials = 300;
        int covered = 0;
        double maxAbsErr = 0;
        for (int k = 0; k < trials; k++) {
            Calibration cal = Calibration.fromEdgePoints(scene.stickerEdgePoints(24, 0.5, seeds.nextLong()),
                    Calibration.Method.MANUAL, face.stickerDiameterMm);
            BilateralMeasurements m = MeasurementEngine.measureBoth(scene.landmarksPx(Eye.OD),
                    scene.landmarksPx(Eye.OS), cal.scale());
            double err = m.ipd.value - truth.ipd();
            maxAbsErr = Math.max(maxAbsErr, Math.abs(err));
            if (Math.abs(err) <= 2 * m.ipd.sigmaCal) {
                covered++;
            }
        }
        double coverage = covered / (double) trials;
        assertTrue(coverage > 0.90 && coverage < 0.99, "coverage " + coverage);
        // With a 600 px sticker and 24 careful clicks even the longest distance stays near 0.1 mm.
        assertTrue(maxAbsErr < 0.15, "max IPD error " + maxAbsErr);
    }

    @Test
    void groundTruthOfTypicalFaceMatchesHandValues() {
        GroundTruth t = GroundTruth.of(FaceModel.typical());
        Map<MeasureId, Double> od = t.perEye(Eye.OD);
        assertEquals(4.0, od.get(MeasureId.MRD1), 1e-12);
        assertEquals(5.5, od.get(MeasureId.MRD2), 1e-12);
        assertEquals(9.5, od.get(MeasureId.PFH), 1e-12);
        assertEquals(Math.hypot(28.5, 2.0), od.get(MeasureId.PFW), 1e-12);
        assertEquals(Math.toDegrees(Math.atan2(2.0, 28.5)), od.get(MeasureId.CANTHAL_ANGLE), 1e-12);
        assertEquals(62.5, t.ipd(), 1e-12);
        assertEquals(Math.hypot(33.3, 0.2), t.icd(), 1e-12);
    }

    @Test
    void ptosisFaceHasNegativeMrd1OnTheRight() {
        assertTrue(GroundTruth.of(FaceModel.rightPtosis()).perEye(Eye.OD).get(MeasureId.MRD1) < 0);
    }

    @Test
    void sceneEdgePointsLieOnTheStickerEllipse() {
        SyntheticScene scene = SyntheticScene.of(FaceModel.typical(), 0.05, 12, true, 0.9);
        for (Vec2 p : scene.stickerEdgePoints(30, 0, 0)) {
            assertEquals(0, scene.stickerPx().signedDistance(p), 1e-9);
        }
        assertEquals(24.0 / 0.05, scene.stickerPx().majorAxis(), 1e-9);
        assertEquals(0.9, scene.stickerPx().axisRatio(), 1e-12);
    }
}
