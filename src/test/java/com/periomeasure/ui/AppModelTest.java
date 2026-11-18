package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Vec2;
import com.periomeasure.core.Warning;
import com.periomeasure.io.Settings;
import com.periomeasure.session.AxisMode;
import com.periomeasure.session.Handle;
import com.periomeasure.session.Landmark;
import com.periomeasure.session.OptionalLandmark;
import com.periomeasure.session.SessionSetup;
import com.periomeasure.session.Step;
import com.periomeasure.synthetic.FaceModel;
import com.periomeasure.synthetic.GroundTruth;
import com.periomeasure.synthetic.SyntheticScene;

class AppModelTest {

    private final SyntheticScene scene = SyntheticScene.of(FaceModel.typical(), 0.05, 4, false, 0.98);
    private File root;
    private AppModel model;

    @BeforeEach
    void setUp() {
        root = new File("target/test-output/app/" + System.nanoTime());
        model = new AppModel(Settings.defaults().withOutputDir(new File(root, "out").getPath()),
                Collections.<String, String>emptyMap(), new File(root, "local"));
        model.imageLoaded(null, scene.width, scene.height, Collections.<Warning>emptyList(),
                GroundTruth.of(scene.face));
    }

    private static SessionSetup both() {
        return new SessionSetup("P1", LocalDate.of(2026, 3, 2), Timing.PRE_OP, EyeSelection.BOTH,
                AxisMode.IMAGE_HORIZONTAL, EnumSet.noneOf(OptionalLandmark.class));
    }

    private Vec2 target(Step st) {
        EyeLandmarks lm = scene.landmarksPx(st.eye);
        switch (st.landmark) {
            case PUPIL:
                return lm.pupil;
            case UPPER_LID:
                return lm.upperLid;
            case LOWER_LID:
                return lm.lowerLid;
            case MEDIAL_CANTHUS:
                return lm.medialCanthus;
            default:
                return lm.lateralCanthus;
        }
    }

    @Test
    void phasesFollowTheWorkflow() {
        assertEquals(AppModel.Phase.SETUP, model.phase());
        model.start(both());
        assertEquals(AppModel.Phase.MEASURING, model.phase());
        assertEquals(Landmark.CALIBRATION_EDGE, model.session().current().landmark);
    }

    @Test
    void oneClickPerLandmarkAdvancesAutomaticallyAndFinishingMatchesTruth() {
        model.start(both());
        for (Vec2 p : scene.stickerEdgePoints(16, 0, 1)) {
            assertEquals(AppModel.PlaceResult.PLACED, model.place(p)); // open-ended: no auto-advance
        }
        model.next();
        int clicks = 0;
        while (true) {
            Step st = model.session().current();
            boolean last = model.session().isLast();
            AppModel.PlaceResult r = model.place(target(st));
            clicks++;
            if (last) {
                assertEquals(AppModel.PlaceResult.PLACED, r);
                break;
            }
            assertEquals(AppModel.PlaceResult.ADVANCED, r, st.toString());
        }
        assertEquals(10, clicks); // 2 pupils + 2 x (upper, lower, MC, LC): one click each, no "Next"
        AppModel.Outcome o = model.finish(LocalDateTime.of(2026, 11, 18, 12, 0));
        assertTrue(o.ok, o.error);
        assertEquals(AppModel.Phase.RESULTS, model.phase());
        assertTrue(o.files.sessionFile.isFile());
        assertEquals(GroundTruth.of(scene.face).icd(), o.record.bilateral.icd.value, 1e-6);
        assertNotNull(model.truth());
    }

    @Test
    void theLastPlacedPointIsSelectedAndCanBeRefinedInTheLoupe() {
        model.start(both());
        for (Vec2 p : scene.stickerEdgePoints(8, 0, 1)) {
            model.place(p);
        }
        model.next();
        model.place(new Vec2(500.2, 400.7));
        Handle sel = model.selected();
        assertEquals(new Vec2(500.2, 400.7), model.session().points(sel.stepIndex).get(sel.pointIndex));
        assertEquals(new Vec2(500.2, 400.7), model.loupeCenter(null));
        assertEquals(new Vec2(10, 10), model.loupeCenter(new Vec2(10, 10)));

        model.beginRefine();
        model.refineTo(new Vec2(501.125, 399.875));
        model.refineTo(new Vec2(501.25, 400.0));
        assertEquals(new Vec2(501.25, 400.0), model.session().points(sel.stepIndex).get(sel.pointIndex));
        model.undo();
        assertEquals(new Vec2(500.2, 400.7), model.session().points(sel.stepIndex).get(sel.pointIndex));
    }

    @Test
    void pressingOnAPointDragsItInsteadOfPlacingANewOne() {
        model.start(both());
        for (Vec2 p : scene.stickerEdgePoints(8, 0, 1)) {
            model.place(p);
        }
        model.next();
        model.place(new Vec2(300, 300)); // pupil OD, advances to pupil OS
        Handle h = model.hit(new Vec2(302, 301), 4);
        assertNotNull(h);
        model.startDrag(h);
        model.dragTo(new Vec2(310, 305));
        model.endDrag();
        assertEquals(new Vec2(310, 305), model.session().points(h.stepIndex).get(h.pointIndex));
        assertEquals(h, model.selected());
        assertTrue(model.session().points(model.session().currentIndex()).isEmpty(), "OS pupil still empty");
        model.dragTo(new Vec2(1, 1)); // no drag in progress: ignored
        assertEquals(new Vec2(310, 305), model.session().points(h.stepIndex).get(h.pointIndex));
    }

    @Test
    void draggedPointsStayInsideTheImage() {
        model.start(both());
        model.place(new Vec2(10, 10));
        model.startDrag(model.selected());
        model.dragTo(new Vec2(-50, scene.height + 80));
        Vec2 p = model.session().points(0).get(0);
        assertEquals(0, p.x, 0);
        assertEquals(scene.height, p.y, 0);
    }

    @Test
    void undoSelectsTheLastRemainingPoint() {
        model.start(both());
        model.place(new Vec2(10, 10));
        model.place(new Vec2(20, 20));
        model.undo();
        assertEquals(new Handle(0, 0), model.selected());
        model.undo();
        assertNull(model.selected());
    }

    @Test
    void computationErrorsAreReportedAndTheSessionStaysOpen() {
        model.start(both());
        for (int i = 0; i < 8; i++) {
            model.place(new Vec2(10 + i, 20 + 2 * i)); // collinear sticker points
        }
        model.next();
        for (int i = 0; i < 10; i++) {
            model.place(new Vec2(100 + 10 * i, 300 + (i % 3)));
        }
        AppModel.Outcome o = model.finish(LocalDateTime.now());
        assertFalse(o.ok);
        assertNotNull(o.error);
        assertEquals(AppModel.Phase.MEASURING, model.phase());
    }

    @Test
    void photoInACloudFolderWritesResultsToTheLocalFolderInstead() {
        Map<String, String> env = new HashMap<>();
        File cloud = new File(root, "OneDrive");
        env.put("OneDrive", cloud.getAbsolutePath());
        AppModel m = new AppModel(Settings.defaults(), env, new File(root, "local"));
        m.imageLoaded(new File(cloud, "P1_pre.jpg"), 100, 100, Collections.<Warning>emptyList(), null);
        assertEquals(new File(root, "local"), m.outputDir());
        AppModel explicit = new AppModel(Settings.defaults().withOutputDir(new File(cloud, "out").getPath()), env,
                new File(root, "local"));
        explicit.imageLoaded(null, 100, 100, Collections.<Warning>emptyList(), null);
        assertEquals(new File(cloud, "out"), explicit.outputDir());
        assertTrue(explicit.outputWarnings().stream()
                .anyMatch(w -> w.code == com.periomeasure.core.WarningCode.CLOUD_FOLDER_OUTPUT));
    }

    @Test
    void demoOutputFolderOverridesOnlyForThatImage() {
        File demo = new File(root, "demo-output");
        model.imageLoaded(null, 100, 100, Collections.<Warning>emptyList(), null, demo);
        assertEquals(demo, model.outputDir());
        model.imageLoaded(null, 100, 100, Collections.<Warning>emptyList(), null);
        assertEquals(new File(root, "out"), model.outputDir());
    }

    @Test
    void newMeasurementKeepsTheImageAndReturnsToSetup() {
        model.start(both());
        model.newMeasurement();
        assertEquals(AppModel.Phase.SETUP, model.phase());
        assertNull(model.session());
        List<MeasureId> none = Collections.emptyList();
        assertTrue(none.isEmpty());
    }
}
