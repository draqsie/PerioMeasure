package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class CalibrationTest {

    private static List<Vec2> edge(double a, double b, double theta, int n) {
        return EllipseFitterTest.sample(new Ellipse(1200, 400, a, b, theta), n, 0.05, 2 * Math.PI, 0, null);
    }

    private static boolean has(Calibration c, WarningCode code) {
        for (Warning w : c.warnings()) {
            if (w.code == code) {
                return true;
            }
        }
        return false;
    }

    @Test
    void defaultReferenceIs24mm() {
        assertEquals(24.0, Calibration.DEFAULT_REFERENCE_DIAMETER_MM, 0);
        assertEquals(8, Calibration.MIN_MANUAL_POINTS);
    }

    @Test
    void circleGivesMmPerPxFromDiameter() {
        Calibration c = Calibration.fromEdgePoints(edge(120, 120, 0, 16), Calibration.Method.MANUAL, 24.0);
        assertEquals(240, c.majorAxisPx(), 1e-9);
        assertEquals(0.1, c.mmPerPx(), 1e-12);
        assertEquals(1.0, c.axisRatio(), 1e-9);
        assertTrue(c.warnings().isEmpty(), c.warnings().toString());
        assertEquals(Calibration.Method.MANUAL, c.method);
        assertEquals(16, c.pointCount);
    }

    @Test
    void obliqueStickerUsesTheUnforeshortenedMajorAxis() {
        Calibration c = Calibration.fromEdgePoints(edge(120, 110, 1.3, 16), Calibration.Method.MANUAL, 24.0);
        assertEquals(24.0 / 240.0, c.mmPerPx(), 1e-12);
        assertEquals(220, c.minorAxisPx(), 1e-9);
        assertTrue(has(c, WarningCode.OBLIQUE_VIEW));
    }

    @Test
    void obliqueWarningThresholdIs097() {
        assertTrue(!has(Calibration.fromEdgePoints(edge(200, 195, 0.2, 12), Calibration.Method.MANUAL, 24), WarningCode.OBLIQUE_VIEW));
        assertTrue(has(Calibration.fromEdgePoints(edge(200, 193, 0.2, 12), Calibration.Method.MANUAL, 24), WarningCode.OBLIQUE_VIEW));
    }

    @Test
    void smallStickerTriggersLowResolutionWarning() {
        Calibration small = Calibration.fromEdgePoints(edge(70, 70, 0, 12), Calibration.Method.MANUAL, 24);
        assertTrue(has(small, WarningCode.LOW_RESOLUTION));
        Calibration ok = Calibration.fromEdgePoints(edge(75.001, 75.001, 0, 12), Calibration.Method.MANUAL, 24);
        assertTrue(!has(ok, WarningCode.LOW_RESOLUTION));
    }

    @Test
    void warningCarriesTheOffendingValue() {
        Calibration c = Calibration.fromEdgePoints(edge(70, 63, 0.4, 12), Calibration.Method.MANUAL, 24);
        for (Warning w : c.warnings()) {
            if (w.code == WarningCode.LOW_RESOLUTION) {
                assertEquals(140, w.value, 1e-6);
            }
            if (w.code == WarningCode.OBLIQUE_VIEW) {
                assertEquals(0.9, w.value, 1e-9);
            }
        }
        assertEquals(2, c.warnings().size());
    }

    @Test
    void manualCalibrationNeedsAtLeastEightPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> Calibration.fromEdgePoints(edge(120, 120, 0, 7), Calibration.Method.MANUAL, 24));
    }

    @Test
    void referenceDiameterIsConfigurable() {
        Calibration c = Calibration.fromEdgePoints(edge(120, 120, 0, 10), Calibration.Method.MANUAL, 20.0);
        assertEquals(20.0 / 240.0, c.mmPerPx(), 1e-12);
        assertEquals(20.0, c.referenceDiameterMm, 0);
    }

    @Test
    void invalidReferenceDiameterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Calibration.fromEdgePoints(edge(120, 120, 0, 10), Calibration.Method.MANUAL, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Calibration.fromEdgePoints(edge(120, 120, 0, 10), Calibration.Method.MANUAL, Double.NaN));
    }

    @Test
    void relativeUncertaintyAndRmsComeFromTheFit() {
        Ellipse truth = new Ellipse(900, 500, 160, 158, 0.3);
        List<Vec2> pts = EllipseFitterTest.sample(truth, 20, 0, 2 * Math.PI, 0.7, new Random(4));
        Calibration c = Calibration.fromEdgePoints(pts, Calibration.Method.MANUAL, 24);
        EllipseFitResult fit = EllipseFitter.fit(pts);
        assertEquals(fit.sigmaMajorAxisPx / fit.ellipse.majorAxis(), c.relativeUncertainty(), 1e-15);
        assertEquals(fit.rmsResidualPx, c.rmsResidualPx, 1e-15);
        assertTrue(c.relativeUncertainty() > 0);
        Scale s = c.scale();
        assertEquals(c.mmPerPx(), s.mmPerPx, 0);
        assertEquals(c.relativeUncertainty(), s.relativeSigma, 0);
    }

    @Test
    void scaleRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new Scale(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Scale(0.1, -1e-3));
        assertThrows(IllegalArgumentException.class, () -> new Scale(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Scale(0.1, Double.NaN));
    }
}
