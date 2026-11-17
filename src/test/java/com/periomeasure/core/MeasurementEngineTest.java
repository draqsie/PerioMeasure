package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class MeasurementEngineTest {

    private static final double EPS = 1e-9;
    private static final Scale TENTH = new Scale(0.1, 0);

    /** Right eye (on the image left), image axes. Values below are derived by hand. */
    private static EyeLandmarks.Builder od() {
        return EyeLandmarks.builder()
                .pupil(new Vec2(100, 100))
                .upperLid(new Vec2(100, 90))
                .lowerLid(new Vec2(100, 106))
                .medialCanthus(new Vec2(115, 101))
                .lateralCanthus(new Vec2(75, 98));
    }

    /** Left eye: exact mirror image of {@link #od()} about x = 200. */
    private static EyeLandmarks.Builder os() {
        return EyeLandmarks.builder()
                .pupil(new Vec2(300, 100))
                .upperLid(new Vec2(300, 90))
                .lowerLid(new Vec2(300, 106))
                .medialCanthus(new Vec2(285, 101))
                .lateralCanthus(new Vec2(325, 98));
    }

    private static double v(EyeMeasurements m, MeasureId id) {
        return m.get(id).value;
    }

    private static boolean has(List<Warning> ws, WarningCode code) {
        for (Warning w : ws) {
            if (w.code == code) {
                return true;
            }
        }
        return false;
    }

    @Test
    void rightEyeHandComputedValues() {
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD, od().build(), ReferenceAxes.imageHorizontal(), TENTH);
        double pfw = Math.hypot(40, 3) * 0.1;
        assertEquals(pfw, v(m, MeasureId.PFW), EPS);
        assertEquals(1.6, v(m, MeasureId.PFH), EPS);
        assertEquals(1.0, v(m, MeasureId.MRD1), EPS);
        assertEquals(0.6, v(m, MeasureId.MRD2), EPS);
        assertEquals(1.6 / pfw, v(m, MeasureId.PFI), EPS);
        // LC is 3 px higher than MC (y smaller) and 40 px lateral: positive tilt.
        assertEquals(Math.toDegrees(Math.atan2(3, 40)), v(m, MeasureId.CANTHAL_ANGLE), EPS);
        assertTrue(m.warnings().isEmpty(), m.warnings().toString());
        assertEquals(Eye.OD, m.eye);
    }

    @Test
    void mirroredLeftEyeGivesIdenticalValuesIncludingAngleSign() {
        EyeMeasurements r = MeasurementEngine.measureEye(Eye.OD, od().build(), ReferenceAxes.imageHorizontal(), TENTH);
        EyeMeasurements l = MeasurementEngine.measureEye(Eye.OS, os().build(), ReferenceAxes.imageHorizontal(), TENTH);
        for (MeasureId id : new MeasureId[] {MeasureId.PFW, MeasureId.PFH, MeasureId.MRD1, MeasureId.MRD2,
                MeasureId.PFI, MeasureId.CANTHAL_ANGLE}) {
            assertEquals(v(r, id), v(l, id), EPS, id.name());
        }
        assertTrue(v(l, MeasureId.CANTHAL_ANGLE) > 0);
    }

    @Test
    void lateralCanthusBelowMedialGivesNegativeAngle() {
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD,
                od().lateralCanthus(new Vec2(75, 105)).build(), ReferenceAxes.imageHorizontal(), TENTH);
        assertEquals(-Math.toDegrees(Math.atan2(4, 40)), v(m, MeasureId.CANTHAL_ANGLE), EPS);
    }

    @Test
    void ptosisGivesNegativeMrd1() {
        // Upper lid margin 3 px BELOW the light reflex.
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD,
                od().upperLid(new Vec2(100, 103)).build(), ReferenceAxes.imageHorizontal(), TENTH);
        assertEquals(-0.3, v(m, MeasureId.MRD1), EPS);
        assertEquals(0.6, v(m, MeasureId.MRD2), EPS);
        assertEquals(0.3, v(m, MeasureId.PFH), EPS);
        assertFalse(has(m.warnings(), WarningCode.MRD_INCONSISTENT));
    }

    @Test
    void valuesAreInvariantUnderRotationAndTranslationOfTheWholeFace() {
        BilateralMeasurements ref = MeasurementEngine.measureBoth(od().build(), os().build(), TENTH);
        Random rnd = new Random(12);
        for (int k = 0; k < 200; k++) {
            double phi = Math.toRadians(-40 + 80 * rnd.nextDouble());
            Vec2 shift = new Vec2(2000 * rnd.nextDouble(), 2000 * rnd.nextDouble());
            BilateralMeasurements m = MeasurementEngine.measureBoth(
                    transform(od().build(), phi, shift), transform(os().build(), phi, shift), TENTH);
            for (MeasureId id : MeasureId.values()) {
                if (ref.od.has(id)) {
                    assertEquals(ref.od.get(id).value, m.od.get(id).value, EPS, "OD " + id);
                    assertEquals(ref.os.get(id).value, m.os.get(id).value, EPS, "OS " + id);
                }
            }
            assertEquals(ref.icd.value, m.icd.value, EPS);
            assertEquals(ref.ipd.value, m.ipd.value, EPS);
        }
    }

    @Test
    void lengthsScaleLinearlyWhileRatioAndAngleDoNot() {
        EyeMeasurements a = MeasurementEngine.measureEye(Eye.OD, od().build(), ReferenceAxes.imageHorizontal(), TENTH);
        EyeMeasurements b = MeasurementEngine.measureEye(Eye.OD, od().build(), ReferenceAxes.imageHorizontal(),
                new Scale(0.2, 0));
        assertEquals(2 * v(a, MeasureId.PFW), v(b, MeasureId.PFW), EPS);
        assertEquals(2 * v(a, MeasureId.MRD1), v(b, MeasureId.MRD1), EPS);
        assertEquals(v(a, MeasureId.PFI), v(b, MeasureId.PFI), EPS);
        assertEquals(v(a, MeasureId.CANTHAL_ANGLE), v(b, MeasureId.CANTHAL_ANGLE), EPS);
    }

    @Test
    void mrdSumAlwaysEqualsPfh() {
        Random rnd = new Random(8);
        for (int k = 0; k < 500; k++) {
            EyeLandmarks lm = EyeLandmarks.builder()
                    .pupil(rndPoint(rnd)).upperLid(rndPoint(rnd)).lowerLid(rndPoint(rnd))
                    .medialCanthus(new Vec2(900, 500)).lateralCanthus(new Vec2(500, 520)).build();
            ReferenceAxes ax = ReferenceAxes.fromLine(new Vec2(0, 0), new Vec2(100, 30 * rnd.nextDouble() - 15));
            EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD, lm, ax, TENTH);
            assertEquals(v(m, MeasureId.PFH), v(m, MeasureId.MRD1) + v(m, MeasureId.MRD2), 1e-9);
            assertFalse(has(m.warnings(), WarningCode.MRD_INCONSISTENT));
        }
    }

    @Test
    void canthusOrderWarning() {
        // Lateral canthus placed on the nasal side of the medial canthus.
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD,
                od().lateralCanthus(new Vec2(140, 98)).build(), ReferenceAxes.imageHorizontal(), TENTH);
        assertTrue(has(m.warnings(), WarningCode.CANTHUS_ORDER));
    }

    @Test
    void upperLidBelowLowerLidWarns() {
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD,
                od().upperLid(new Vec2(100, 108)).build(), ReferenceAxes.imageHorizontal(), TENTH);
        assertTrue(has(m.warnings(), WarningCode.NONPOSITIVE_PFH));
    }

    @Test
    void lidPointFarFromPupilVerticalWarns() {
        EyeMeasurements far = MeasurementEngine.measureEye(Eye.OD,
                od().upperLid(new Vec2(115, 90)).build(), ReferenceAxes.imageHorizontal(), TENTH);
        assertTrue(has(far.warnings(), WarningCode.LID_POINT_OFF_PUPIL_AXIS)); // 1.5 mm off
        EyeMeasurements near = MeasurementEngine.measureEye(Eye.OD,
                od().upperLid(new Vec2(105, 90)).build(), ReferenceAxes.imageHorizontal(), TENTH);
        assertFalse(has(near.warnings(), WarningCode.LID_POINT_OFF_PUPIL_AXIS)); // 0.5 mm off
        // The off-axis offset never changes the vertical measurements.
        assertEquals(1.0, v(near, MeasureId.MRD1), EPS);
    }

    @Test
    void lengthUncertaintyCombinesClickAndCalibration() {
        Scale s = new Scale(0.1, 0.002);
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD, od().build(), ReferenceAxes.imageHorizontal(), s);
        Quantity pfw = m.get(MeasureId.PFW);
        assertEquals(0.1, pfw.sigmaClick, EPS);              // 1 px x mm/px
        assertEquals(pfw.value * 0.002, pfw.sigmaCal, EPS);   // L x relative calibration sigma
        assertEquals(Math.hypot(pfw.sigmaClick, pfw.sigmaCal), pfw.sigmaTotal(), EPS);
        Quantity mrd1 = m.get(MeasureId.MRD1);
        assertEquals(Math.abs(mrd1.value) * 0.002, mrd1.sigmaCal, EPS);
        assertEquals(Unit.MM, pfw.unit);
    }

    @Test
    void ratioAndAngleUncertainty() {
        Scale s = new Scale(0.1, 0.002);
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD, od().build(), ReferenceAxes.imageHorizontal(), s);
        double h = 1.6;
        double w = Math.hypot(40, 3) * 0.1;
        Quantity pfi = m.get(MeasureId.PFI);
        assertEquals(Math.sqrt(Math.pow(0.1 / w, 2) + Math.pow(h * 0.1 / (w * w), 2)), pfi.sigmaClick, EPS);
        assertEquals(0, pfi.sigmaCal, 0); // the scale cancels in a ratio
        assertEquals(Unit.RATIO, pfi.unit);
        Quantity ang = m.get(MeasureId.CANTHAL_ANGLE);
        assertEquals(Math.toDegrees(Math.atan(1 / Math.hypot(40, 3))), ang.sigmaClick, EPS);
        assertEquals(0, ang.sigmaCal, 0);
        assertEquals(Unit.DEGREE, ang.unit);
    }

    @Test
    void fissureAreaFromContour() {
        List<Vec2> contour = Arrays.asList(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 10), new Vec2(0, 10));
        Scale s = new Scale(0.1, 0.002);
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD, od().contour(contour).build(),
                ReferenceAxes.imageHorizontal(), s);
        Quantity area = m.get(MeasureId.FISSURE_AREA);
        assertEquals(1.0, area.value, EPS);
        assertEquals(2 * 1.0 * 0.002, area.sigmaCal, EPS);
        assertEquals(40 * 0.1 * 0.1, area.sigmaClick, EPS); // 1 px band along the perimeter
        assertEquals(Unit.MM2, area.unit);
    }

    @Test
    void optionalMeasurementsAreAbsentUnlessTheirLandmarksAreGiven() {
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD, od().build(), ReferenceAxes.imageHorizontal(), TENTH);
        for (MeasureId id : new MeasureId[] {MeasureId.FISSURE_AREA, MeasureId.TPS, MeasureId.BROW_HEIGHT,
                MeasureId.SUPERIOR_SCLERAL_SHOW, MeasureId.INFERIOR_SCLERAL_SHOW}) {
            assertFalse(m.has(id), id.name());
            assertNull(m.get(id));
        }
    }

    @Test
    void optionalMeasurementsWhenGiven() {
        EyeMeasurements m = MeasurementEngine.measureEye(Eye.OD, od()
                .upperLidCrease(new Vec2(100, 60))    // 30 px above the upper lid
                .browLowerBorder(new Vec2(100, 20))   // 80 px above the pupil
                .limbusUpper(new Vec2(100, 94))       // lid margin 4 px above: superior scleral show
                .limbusLower(new Vec2(100, 103))      // lower lid 3 px below: inferior scleral show
                .build(), ReferenceAxes.imageHorizontal(), TENTH);
        assertEquals(3.0, v(m, MeasureId.TPS), EPS);
        assertEquals(8.0, v(m, MeasureId.BROW_HEIGHT), EPS);
        assertEquals(0.4, v(m, MeasureId.SUPERIOR_SCLERAL_SHOW), EPS);
        assertEquals(0.3, v(m, MeasureId.INFERIOR_SCLERAL_SHOW), EPS);
    }

    @Test
    void bilateralDistancesAndDifferences() {
        Scale s = new Scale(0.1, 0.002);
        BilateralMeasurements b = MeasurementEngine.measureBoth(od().build(),
                os().lateralCanthus(new Vec2(323, 98)).build(), s);
        assertEquals(17.0, b.icd.value, EPS);   // 285 - 115 = 170 px
        assertEquals(20.0, b.ipd.value, EPS);   // 300 - 100 = 200 px
        assertEquals(17.0 * 0.002, b.icd.sigmaCal, EPS);
        assertEquals(ReferenceAxes.Source.PUPILS, b.axes.source);
        Quantity dPfw = b.difference(MeasureId.PFW);
        double pfwOd = Math.hypot(40, 3) * 0.1;
        double pfwOs = Math.hypot(38, 3) * 0.1;
        assertEquals(pfwOd - pfwOs, dPfw.value, EPS);
        assertEquals(Math.hypot(0.1, 0.1), dPfw.sigmaClick, EPS);
        // Both eyes share one scale, so its error is fully correlated: sigma = |difference| x rel.
        assertEquals(Math.abs(pfwOd - pfwOs) * 0.002, dPfw.sigmaCal, EPS);
        assertEquals(0, b.difference(MeasureId.MRD1).value, EPS);
        assertTrue(b.warnings().isEmpty(), b.warnings().toString());
    }

    @Test
    void bilateralOnMirroredImageWarns() {
        BilateralMeasurements b = MeasurementEngine.measureBoth(
                mirror(od().build()), mirror(os().build()), TENTH);
        assertTrue(has(b.warnings(), WarningCode.IMAGE_MIRRORED));
        // Values are still computed correctly with the anatomical axes.
        BilateralMeasurements ref = MeasurementEngine.measureBoth(od().build(), os().build(), TENTH);
        assertEquals(ref.od.get(MeasureId.CANTHAL_ANGLE).value, b.od.get(MeasureId.CANTHAL_ANGLE).value, EPS);
        assertEquals(ref.os.get(MeasureId.MRD1).value, b.os.get(MeasureId.MRD1).value, EPS);
    }

    @Test
    void missingRequiredLandmarkIsRejected() {
        assertThrows(IllegalStateException.class, () -> EyeLandmarks.builder()
                .pupil(new Vec2(1, 1)).upperLid(new Vec2(1, 0)).lowerLid(new Vec2(1, 2))
                .medialCanthus(new Vec2(3, 1)).build());
    }

    @Test
    void contourNeedsThreePoints() {
        assertThrows(IllegalArgumentException.class,
                () -> od().contour(Arrays.asList(new Vec2(0, 0), new Vec2(1, 0))).build());
    }

    @Test
    void coincidentCanthiAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MeasurementEngine.measureEye(Eye.OD,
                od().lateralCanthus(new Vec2(115, 101)).build(), ReferenceAxes.imageHorizontal(), TENTH));
    }

    // ------------------------------------------------------------------ helpers

    private static Vec2 rndPoint(Random rnd) {
        return new Vec2(500 + 400 * rnd.nextDouble(), 400 + 200 * rnd.nextDouble());
    }

    private static Vec2 rot(Vec2 p, double phi, Vec2 shift) {
        double c = Math.cos(phi);
        double s = Math.sin(phi);
        return new Vec2(c * p.x - s * p.y, s * p.x + c * p.y).plus(shift);
    }

    private static EyeLandmarks transform(EyeLandmarks e, double phi, Vec2 shift) {
        return EyeLandmarks.builder()
                .pupil(rot(e.pupil, phi, shift)).upperLid(rot(e.upperLid, phi, shift))
                .lowerLid(rot(e.lowerLid, phi, shift)).medialCanthus(rot(e.medialCanthus, phi, shift))
                .lateralCanthus(rot(e.lateralCanthus, phi, shift)).build();
    }

    private static EyeLandmarks mirror(EyeLandmarks e) {
        List<Vec2> pts = new ArrayList<>(Arrays.asList(e.pupil, e.upperLid, e.lowerLid, e.medialCanthus,
                e.lateralCanthus));
        List<Vec2> m = new ArrayList<>();
        for (Vec2 p : pts) {
            m.add(new Vec2(1000 - p.x, p.y));
        }
        return EyeLandmarks.builder().pupil(m.get(0)).upperLid(m.get(1)).lowerLid(m.get(2))
                .medialCanthus(m.get(3)).lateralCanthus(m.get(4)).build();
    }
}
