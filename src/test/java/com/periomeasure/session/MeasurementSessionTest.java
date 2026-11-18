package com.periomeasure.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Vec2;
import com.periomeasure.core.Warning;
import com.periomeasure.io.MeasurementRecord;
import com.periomeasure.io.ResultCsv;
import com.periomeasure.synthetic.FaceModel;
import com.periomeasure.synthetic.GroundTruth;
import com.periomeasure.synthetic.SyntheticScene;

class MeasurementSessionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 0, 0);

    static SessionSetup setup(EyeSelection eyes, AxisMode axis, OptionalLandmark... optional) {
        EnumSet<OptionalLandmark> opt = EnumSet.noneOf(OptionalLandmark.class);
        opt.addAll(Arrays.asList(optional));
        return new SessionSetup("P017", LocalDate.of(2026, 3, 2), Timing.PRE_OP, eyes, axis, opt);
    }

    private static MeasurementSession both() {
        return new MeasurementSession(setup(EyeSelection.BOTH, AxisMode.IMAGE_HORIZONTAL), 24.0);
    }

    private static List<String> names(List<Step> steps) {
        String[] s = new String[steps.size()];
        for (int i = 0; i < s.length; i++) {
            s[i] = steps.get(i).toString();
        }
        return Arrays.asList(s);
    }

    // ------------------------------------------------------------------ step plan

    @Test
    void bothEyesPlanMarksBothPupilsFirstSoTheAxisIsKnownForTheLids() {
        assertEquals(Arrays.asList("CALIBRATION_EDGE", "PUPIL[OD]", "PUPIL[OS]",
                "UPPER_LID[OD]", "LOWER_LID[OD]", "MEDIAL_CANTHUS[OD]", "LATERAL_CANTHUS[OD]",
                "UPPER_LID[OS]", "LOWER_LID[OS]", "MEDIAL_CANTHUS[OS]", "LATERAL_CANTHUS[OS]"),
                names(StepPlan.build(setup(EyeSelection.BOTH, AxisMode.IMAGE_HORIZONTAL))));
    }

    @Test
    void singleEyeWithUserLineAndAllOptionalLandmarks() {
        assertEquals(Arrays.asList("CALIBRATION_EDGE", "REFERENCE_LINE", "PUPIL[OS]",
                "UPPER_LID[OS]", "LOWER_LID[OS]", "MEDIAL_CANTHUS[OS]", "LATERAL_CANTHUS[OS]",
                "UPPER_LID_CREASE[OS]", "BROW_LOWER_BORDER[OS]", "LIMBUS_UPPER[OS]", "LIMBUS_LOWER[OS]",
                "CONTOUR[OS]"),
                names(StepPlan.build(setup(EyeSelection.OS, AxisMode.USER_LINE, OptionalLandmark.values()))));
    }

    @Test
    void singleEyeWithImageHorizontalHasNoLineStep() {
        assertFalse(names(StepPlan.build(setup(EyeSelection.OD, AxisMode.IMAGE_HORIZONTAL))).contains("REFERENCE_LINE"));
    }

    // ------------------------------------------------------------------ setup validation

    @Test
    void setupRequiresAPatientCode() {
        assertThrows(IllegalArgumentException.class, () -> new SessionSetup("  ", LocalDate.of(2026, 1, 1),
                Timing.PRE_OP, EyeSelection.BOTH, AxisMode.IMAGE_HORIZONTAL, EnumSet.noneOf(OptionalLandmark.class)));
        assertEquals("P017", new SessionSetup(" P017 ", LocalDate.of(2026, 1, 1), Timing.PRE_OP, EyeSelection.BOTH,
                AxisMode.IMAGE_HORIZONTAL, EnumSet.noneOf(OptionalLandmark.class)).patientId);
    }

    @Test
    void datesAreParsedStrictly() {
        assertEquals(LocalDate.of(2026, 3, 2), SessionSetup.parseDate("2026-03-02"));
        assertEquals(LocalDate.of(2026, 3, 2), SessionSetup.parseDate(" 2026-03-02 "));
        assertThrows(IllegalArgumentException.class, () -> SessionSetup.parseDate("2026-02-30"));
        assertThrows(IllegalArgumentException.class, () -> SessionSetup.parseDate("2026-3-2"));
        assertThrows(IllegalArgumentException.class, () -> SessionSetup.parseDate("02.03.2026"));
    }

    // ------------------------------------------------------------------ navigation

    @Test
    void calibrationNeedsEightPointsBeforeAdvancing() {
        MeasurementSession s = both();
        for (int i = 0; i < 7; i++) {
            s.place(new Vec2(100 + 50 * Math.cos(i), 100 + 50 * Math.sin(i)));
        }
        assertFalse(s.canAdvance());
        assertThrows(IllegalStateException.class, s::next);
        s.place(new Vec2(100, 150));
        assertTrue(s.canAdvance());
        s.next();
        assertEquals(Landmark.PUPIL, s.current().landmark);
        assertEquals(Eye.OD, s.current().eye);
    }

    @Test
    void singlePointStepIsReplacedByANewClick() {
        MeasurementSession s = atPupilOd();
        s.place(new Vec2(10, 10));
        s.place(new Vec2(12.25, 11.75));
        assertEquals(Collections.singletonList(new Vec2(12.25, 11.75)), s.points(s.currentIndex()));
    }

    @Test
    void referenceLineTakesExactlyTwoPoints() {
        MeasurementSession s = new MeasurementSession(setup(EyeSelection.OD, AxisMode.USER_LINE), 24);
        fillCalibration(s);
        s.next();
        assertEquals(Landmark.REFERENCE_LINE, s.current().landmark);
        assertTrue(s.place(new Vec2(0, 0)));
        assertTrue(s.place(new Vec2(100, 5)));
        assertFalse(s.place(new Vec2(200, 5)));
        assertEquals(2, s.points(s.currentIndex()).size());
    }

    @Test
    void onlyOptionalStepsCanBeSkipped() {
        MeasurementSession s = new MeasurementSession(
                setup(EyeSelection.OD, AxisMode.IMAGE_HORIZONTAL, OptionalLandmark.CREASE), 24);
        assertThrows(IllegalStateException.class, s::skip);
        fillCalibration(s);
        s.next();
        for (int i = 0; i < 5; i++) {
            s.place(new Vec2(10 + i, 10 + i));
            s.next();
        }
        assertEquals(Landmark.UPPER_LID_CREASE, s.current().landmark);
        assertTrue(s.isLast());
        s.place(new Vec2(1, 1));
        s.skip();
        assertTrue(s.points(s.currentIndex()).isEmpty());
        assertTrue(s.canFinish());
    }

    @Test
    void partiallyFilledOptionalContourBlocksFinishing() {
        MeasurementSession s = new MeasurementSession(
                setup(EyeSelection.OD, AxisMode.IMAGE_HORIZONTAL, OptionalLandmark.CONTOUR), 24);
        fillCalibration(s);
        s.next();
        for (int i = 0; i < 5; i++) {
            s.place(new Vec2(10 + i, 10 + 2 * i));
            s.next();
        }
        assertEquals(Landmark.CONTOUR, s.current().landmark);
        s.place(new Vec2(1, 1));
        s.place(new Vec2(5, 1));
        assertFalse(s.canFinish());
        s.place(new Vec2(5, 5));
        assertTrue(s.canFinish());
    }

    @Test
    void previousKeepsThePoints() {
        MeasurementSession s = atPupilOd();
        s.place(new Vec2(3, 4));
        s.next();
        s.previous();
        assertEquals(Landmark.PUPIL, s.current().landmark);
        assertEquals(Collections.singletonList(new Vec2(3, 4)), s.points(s.currentIndex()));
    }

    // ------------------------------------------------------------------ editing

    @Test
    void undoRevertsTheLastEditAndReturnsToItsStep() {
        MeasurementSession s = atPupilOd();
        int pupilStep = s.currentIndex();
        s.place(new Vec2(3, 4));
        s.next();
        s.place(new Vec2(30, 40));
        assertTrue(s.undo());
        assertTrue(s.points(s.currentIndex()).isEmpty());
        assertTrue(s.undo());
        assertEquals(pupilStep, s.currentIndex());
        assertTrue(s.points(pupilStep).isEmpty());
    }

    @Test
    void undoWithNothingToUndo() {
        assertFalse(both().undo());
    }

    @Test
    void dragMovesAPointAndIsUndoable() {
        MeasurementSession s = atPupilOd();
        s.place(new Vec2(50, 50));
        Handle h = s.hit(new Vec2(50.8, 49.5), 1.0);
        assertNotNull(h);
        s.beginDrag(h);
        s.dragTo(h, new Vec2(51.25, 49.75));
        s.dragTo(h, new Vec2(52.5, 48.5));
        assertEquals(new Vec2(52.5, 48.5), s.points(h.stepIndex).get(h.pointIndex));
        assertTrue(s.undo());
        assertEquals(new Vec2(50, 50), s.points(h.stepIndex).get(h.pointIndex));
    }

    @Test
    void hitTestingFindsPointsOfAnyStepButPrefersTheCurrentOne() {
        MeasurementSession s = atPupilOd();
        assertNull(s.hit(new Vec2(1000, 1000), 2));
        Handle cal = s.hit(s.points(0).get(0), 0.5);
        assertEquals(0, cal.stepIndex);
        s.place(s.points(0).get(0)); // pupil placed exactly on a calibration point
        assertEquals(s.currentIndex(), s.hit(s.points(0).get(0), 0.5).stepIndex);
    }

    @Test
    void liveCalibrationFitAppearsFromFivePoints() {
        MeasurementSession s = both();
        for (int i = 0; i < 4; i++) {
            s.place(new Vec2(500 + 100 * Math.cos(i * 1.3), 400 + 100 * Math.sin(i * 1.3)));
        }
        assertNull(s.liveFit());
        s.place(new Vec2(500 + 100 * Math.cos(5.5), 400 + 100 * Math.sin(5.5)));
        assertEquals(200, s.liveFit().ellipse.majorAxis(), 1e-6);
    }

    // ------------------------------------------------------------------ end to end

    /** Click every landmark exactly on a synthetic face, then compare the record and the CSV with ground truth. */
    @Test
    void bothEyesFromSessionToCsvMatchGroundTruth() {
        SyntheticScene scene = SyntheticScene.of(FaceModel.rightPtosis(), 0.04, -6, false, 0.97);
        MeasurementSession s = new MeasurementSession(setup(EyeSelection.BOTH, AxisMode.IMAGE_HORIZONTAL,
                OptionalLandmark.values()), 24.0);
        clickAll(s, scene);
        assertTrue(s.canFinish());
        MeasurementRecord r = s.compute("0.2.0", NOW, "hash", scene.width, scene.height,
                Collections.<Warning>emptyList());
        GroundTruth truth = GroundTruth.of(scene.face);
        for (Eye eye : Eye.values()) {
            for (Map.Entry<MeasureId, Double> e : truth.perEye(eye).entrySet()) {
                assertEquals(e.getValue(), r.perEye.get(eye).get(e.getKey()).value, 1e-6, eye + " " + e.getKey());
            }
        }
        assertEquals(truth.icd(), r.bilateral.icd.value, 1e-6);

        List<String> header = ResultCsv.header();
        List<List<String>> rows = ResultCsv.rows(r, '.');
        for (int i = 0; i < 2; i++) {
            Eye eye = Eye.values()[i];
            for (Map.Entry<MeasureId, Double> e : truth.perEye(eye).entrySet()) {
                double csv = Double.parseDouble(rows.get(i).get(header.indexOf(e.getKey().name())));
                assertEquals(e.getValue(), csv, 1e-4, "CSV " + eye + " " + e.getKey());
            }
        }
    }

    @Test
    void singleEyeWithImageHorizontalOnALevelFace() {
        SyntheticScene scene = SyntheticScene.of(FaceModel.typical(), 0.05, 0, false, 1.0);
        MeasurementSession s = new MeasurementSession(setup(EyeSelection.OS, AxisMode.IMAGE_HORIZONTAL), 24.0);
        clickAll(s, scene);
        MeasurementRecord r = s.compute("0.2.0", NOW, "", scene.width, scene.height, Collections.<Warning>emptyList());
        assertNull(r.bilateral);
        Map<MeasureId, Double> t = GroundTruth.of(scene.face).perEye(Eye.OS);
        for (MeasureId id : new MeasureId[] {MeasureId.PFI, MeasureId.PFW, MeasureId.PFH, MeasureId.MRD1,
                MeasureId.MRD2, MeasureId.CANTHAL_ANGLE}) {
            assertEquals(t.get(id), r.perEye.get(Eye.OS).get(id).value, 1e-6, id.name());
        }
        assertFalse(r.perEye.get(Eye.OS).has(MeasureId.TPS));
    }

    @Test
    void singleEyeWithUserLineOnARolledFace() {
        SyntheticScene scene = SyntheticScene.of(FaceModel.typical(), 0.05, 9, false, 1.0);
        MeasurementSession s = new MeasurementSession(setup(EyeSelection.OD, AxisMode.USER_LINE), 24.0);
        clickAll(s, scene);
        MeasurementRecord r = s.compute("0.2.0", NOW, "", scene.width, scene.height, Collections.<Warning>emptyList());
        assertEquals(9, r.axes.tiltDegrees(), 1e-9);
        Map<MeasureId, Double> t = GroundTruth.of(scene.face).perEye(Eye.OD);
        assertEquals(t.get(MeasureId.MRD1), r.perEye.get(Eye.OD).get(MeasureId.MRD1).value, 1e-6);
        assertEquals(t.get(MeasureId.CANTHAL_ANGLE), r.perEye.get(Eye.OD).get(MeasureId.CANTHAL_ANGLE).value, 1e-6);
    }

    @Test
    void landmarksAreExposedForDrawingGuides() {
        SyntheticScene scene = SyntheticScene.of(FaceModel.typical(), 0.05, 4, false, 1.0);
        MeasurementSession s = both();
        assertNull(s.previewAxes());
        clickAll(s, scene);
        assertEquals(scene.landmarksPx(Eye.OS).pupil, s.pupil(Eye.OS));
        assertEquals(4, s.previewAxes().tiltDegrees(), 1e-9);
    }

    // ------------------------------------------------------------------ helpers

    private static void fillCalibration(MeasurementSession s) {
        for (int i = 0; i < 8; i++) {
            s.place(new Vec2(500 + 100 * Math.cos(i * 0.785), 400 + 100 * Math.sin(i * 0.785)));
        }
    }

    private static MeasurementSession atPupilOd() {
        MeasurementSession s = both();
        fillCalibration(s);
        s.next();
        return s;
    }

    /** Places every landmark of every step at its exact synthetic position and advances. */
    static void clickAll(MeasurementSession s, SyntheticScene scene) {
        while (true) {
            Step st = s.current();
            if (st.landmark == Landmark.CALIBRATION_EDGE) {
                for (Vec2 p : scene.stickerEdgePoints(16, 0, 1)) {
                    s.place(p);
                }
            } else if (st.landmark == Landmark.REFERENCE_LINE) {
                s.place(scene.landmarksPx(Eye.OD).pupil);
                s.place(scene.landmarksPx(Eye.OS).pupil);
            } else if (st.landmark == Landmark.CONTOUR) {
                for (Vec2 p : scene.landmarksPx(st.eye).contour) {
                    s.place(p);
                }
            } else {
                EyeLandmarks lm = scene.landmarksPx(st.eye);
                s.place(landmark(lm, st.landmark));
            }
            if (s.isLast()) {
                return;
            }
            s.next();
        }
    }

    private static Vec2 landmark(EyeLandmarks lm, Landmark l) {
        switch (l) {
            case PUPIL:
                return lm.pupil;
            case UPPER_LID:
                return lm.upperLid;
            case LOWER_LID:
                return lm.lowerLid;
            case MEDIAL_CANTHUS:
                return lm.medialCanthus;
            case LATERAL_CANTHUS:
                return lm.lateralCanthus;
            case UPPER_LID_CREASE:
                return lm.upperLidCrease;
            case BROW_LOWER_BORDER:
                return lm.browLowerBorder;
            case LIMBUS_UPPER:
                return lm.limbusUpper;
            case LIMBUS_LOWER:
                return lm.limbusLower;
            default:
                throw new IllegalArgumentException(l.name());
        }
    }
}
