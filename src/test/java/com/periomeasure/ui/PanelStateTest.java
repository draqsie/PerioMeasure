package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.Vec2;
import com.periomeasure.i18n.Language;
import com.periomeasure.session.AxisMode;
import com.periomeasure.session.MeasurementSession;
import com.periomeasure.session.OptionalLandmark;
import com.periomeasure.session.SessionTestAccess;
import com.periomeasure.synthetic.FaceModel;
import com.periomeasure.synthetic.SyntheticScene;

class PanelStateTest {

    private static MeasurementSession session(OptionalLandmark... opt) {
        return new MeasurementSession(SessionTestAccess.setup(EyeSelection.BOTH, AxisMode.IMAGE_HORIZONTAL, opt), 24);
    }

    private static void circlePoints(MeasurementSession s, int n) {
        for (int i = 0; i < n; i++) {
            s.place(new Vec2(500 + 100 * Math.cos(i * 0.7), 400 + 100 * Math.sin(i * 0.7)));
        }
    }

    @Test
    void calibrationStepWithTooFewPoints() {
        MeasurementSession s = session();
        circlePoints(s, 3);
        PanelState p = PanelState.of(s, Language.TR, 6);
        assertEquals("Adım 1 / 11", p.stepLabel);
        assertEquals("Kalibrasyon etiketi", p.title);
        assertEquals("Nokta sayısı: 3 (en az 8)", p.pointsText);
        assertNull(p.calibrationText);
        assertFalse(p.previousEnabled);
        assertTrue(p.nextVisible);
        assertFalse(p.nextEnabled);
        assertFalse(p.skipVisible);
        assertFalse(p.finishVisible);
        assertTrue(p.help.contains("6×"));
    }

    @Test
    void liveCalibrationTextFromFivePoints() {
        MeasurementSession s = session();
        circlePoints(s, 8);
        PanelState p = PanelState.of(s, Language.TR, 6);
        assertEquals("Etiket çapı 200,0 px · eksen oranı 1,000 · RMS 0,00 px", p.calibrationText);
        assertTrue(p.nextEnabled);
    }

    @Test
    void eyeStepsNameTheEye() {
        MeasurementSession s = session();
        circlePoints(s, 8);
        s.next();
        PanelState p = PanelState.of(s, Language.TR, 6);
        assertEquals("Sağ göz (OD) · Pupil merkezi / ışık refleksi", p.title);
        assertNull(p.pointsText);
        assertTrue(p.previousEnabled);
        assertFalse(p.nextEnabled);
    }

    @Test
    void optionalStepsCanBeSkippedAndTheLastStepFinishes() {
        MeasurementSession s = session(OptionalLandmark.CONTOUR);
        SessionTestAccess.clickAll(s, SyntheticScene.of(FaceModel.typical(), 0.05, 0, false, 1.0));
        PanelState p = PanelState.of(s, Language.EN, 6);
        assertTrue(p.skipVisible);
        assertFalse(p.nextVisible);
        assertTrue(p.finishVisible);
        assertTrue(p.finishEnabled);
        assertTrue(p.readyText.startsWith("All steps done"));
    }
}
