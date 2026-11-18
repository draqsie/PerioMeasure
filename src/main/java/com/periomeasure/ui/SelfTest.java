package com.periomeasure.ui;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.Vec2;
import com.periomeasure.i18n.Language;
import com.periomeasure.io.MeasurementRecord;
import com.periomeasure.session.Handle;
import com.periomeasure.session.Landmark;
import com.periomeasure.session.Step;
import com.periomeasure.synthetic.GroundTruth;
import com.periomeasure.synthetic.SyntheticImageTool;
import com.periomeasure.synthetic.SyntheticScene;

/**
 * Automatic end-to-end check of the real window: opens demo face 2 (tilted
 * head, oblique sticker), places every point with ONE click on the large
 * view, refines it with ONE click in the loupe, checks that the marker is
 * painted on the right pixel in both, presses "Continue" and "Finish", and
 * compares the results with the true values. Run by
 * {@code PerioMeasureApp --selftest <report>}.
 */
final class SelfTest {

    private static final int SCENE = 1;

    private SelfTest() {
    }

    static boolean run(String reportFile) {
        StringBuilder report = new StringBuilder();
        boolean ok;
        shotDir = new File(reportFile).getAbsoluteFile().getParentFile();
        try {
            ok = check(report);
        } catch (Throwable t) {
            report.append("EXCEPTION ").append(t).append('\n');
            for (StackTraceElement e : t.getStackTrace()) {
                report.append("  at ").append(e).append('\n');
            }
            ok = false;
        }
        report.append(ok ? "RESULT: PASS\n" : "RESULT: FAIL\n");
        try {
            File f = new File(reportFile);
            if (f.getParentFile() != null) {
                f.getParentFile().mkdirs();
            }
            Files.write(f.toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // the exit code still reports the result
        }
        return ok;
    }

    private static <T> T edt(Callable<T> c) throws Exception {
        Object[] out = new Object[1];
        Exception[] err = new Exception[1];
        EventQueue.invokeAndWait(() -> {
            try {
                out[0] = c.call();
            } catch (Exception e) {
                err[0] = e;
            }
        });
        if (err[0] != null) {
            throw err[0];
        }
        @SuppressWarnings("unchecked")
        T t = (T) out[0];
        return t;
    }

    /** Saves what the window looks like (for review; never contains patient data: demo face only). */
    private static void snapshot(MainWindow w, File dir, String name) throws Exception {
        if (dir == null) {
            return;
        }
        BufferedImage bi = edt(() -> {
            BufferedImage b = new BufferedImage(w.getRootPane().getWidth(), w.getRootPane().getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            w.getRootPane().paint(b.createGraphics());
            return b;
        });
        javax.imageio.ImageIO.write(bi, "png", new File(dir, name));
    }

    private static File shotDir;

    private static boolean check(StringBuilder report) throws Exception {
        MainWindow w = PerioMeasureApp.openFromAnyThread(false);
        edt(() -> {
            w.loadDemo(SCENE);
            return null;
        });
        SyntheticScene scene = SyntheticImageTool.standardScenes().get(MainWindow.DEMO_SCENES[SCENE]);
        for (int i = 0; i < 100 && edt(() -> w.viewer().view()) == null; i++) {
            Thread.sleep(100);
        }
        Thread.sleep(500);
        AppModel model = w.model();
        int zoom = model.settings().zoomFactor;
        double scale = edt(() -> w.viewer().view().scale);
        report.append(String.format(Locale.ROOT, "image %dx%d, view %dx%d, scale %.4f, loupe %dx%n",
                model.width(), model.height(), w.viewer().getWidth(), w.viewer().getHeight(), scale, zoom));
        double viewTolerance = 0.5 / scale * Math.sqrt(2) + 1e-9;
        double loupeTolerance = 0.5 / zoom * Math.sqrt(2) + 1e-9;
        int clicksOnPhoto = 0;
        int buttonPresses = 0;

        while (true) {
            Step st = edt(() -> model.session().current());
            boolean last = edt(() -> model.session().isLast());
            List<Vec2> targets = targets(scene, st);
            for (Vec2 t : targets) {
                // 1) one click on the large view
                int[] s = edt(() -> w.viewer().view().toScreenPixel(t));
                click(w.viewer(), s[0], s[1]);
                clicksOnPhoto++;
                Handle sel = edt(model::selected);
                Vec2 placed = edt(() -> model.session().points(sel.stepIndex).get(sel.pointIndex));
                if (placed.distanceTo(t) > viewTolerance) {
                    report.append(String.format(Locale.ROOT, "%s: click landed %.3f px away%n", st,
                            placed.distanceTo(t)));
                    return false;
                }
                // 2) one click in the loupe to refine to sub-pixel precision
                int[] ls = edt(() -> ViewState.centeredOn(placed, zoom, LoupeView.SIZE, LoupeView.SIZE)
                        .toScreenPixel(t));
                click(w.loupe(), ls[0], ls[1]);
                Vec2 refined = edt(() -> model.session().points(sel.stepIndex).get(sel.pointIndex));
                if (refined.distanceTo(t) > loupeTolerance) {
                    report.append(String.format(Locale.ROOT, "%s: loupe refinement left %.4f px%n", st,
                            refined.distanceTo(t)));
                    return false;
                }
                if (!markerOk(w, w.viewer(), edt(() -> w.viewer().view()), refined)) {
                    report.append(st).append(": selected marker not painted on its pixel in the photo view\n");
                    return false;
                }
                if (!markerOk(w, w.loupe(), ViewState.centeredOn(refined, zoom, LoupeView.SIZE, LoupeView.SIZE),
                        refined)) {
                    report.append(st).append(": selected marker not painted on its pixel in the loupe\n");
                    return false;
                }
            }
            report.append(String.format(Locale.ROOT, "%-24s %d point(s) OK%n", st, targets.size()));
            if (st.landmark == Landmark.UPPER_LID) {
                snapshot(w, shotDir, "selftest_measuring.png");
            }
            if (last) {
                break;
            }
            if (st.landmark == Landmark.CALIBRATION_EDGE) {
                edt(() -> {
                    w.stepsPanel().next.doClick();
                    return null;
                });
                buttonPresses++;
            }
        }
        edt(() -> {
            w.stepsPanel().finish.doClick();
            return null;
        });
        buttonPresses++;
        if (edt(model::phase) != AppModel.Phase.RESULTS) {
            report.append("finish did not reach the results\n");
            return false;
        }
        Thread.sleep(300);
        snapshot(w, shotDir, "selftest_results.png");
        MeasurementRecord r = edt(() -> model.lastOutcome().record);
        GroundTruth truth = GroundTruth.of(scene.face);
        for (String[] row : ResultsSummary.verificationRows(r, truth, Language.EN)) {
            report.append(String.join(" | ", row)).append('\n');
        }
        report.append(ResultsSummary.verificationSummary(r, truth, Language.EN)).append('\n');
        report.append(String.format(Locale.ROOT, "clicks on the photo: %d (+1 loupe click each), buttons pressed: %d%n",
                clicksOnPhoto, buttonPresses));
        report.append("saved: ").append(edt(() -> model.lastOutcome().files.sessionFile.getAbsolutePath())).append('\n');
        boolean allWithin = true;
        for (String[] row : ResultsSummary.verificationRows(r, truth, Language.EN)) {
            allWithin &= "✓".equals(row[5]);
        }
        edt(() -> {
            w.dispose();
            return null;
        });
        return allWithin && ResultsSummary.maxLengthError(r, truth) < 0.05;
    }

    private static List<Vec2> targets(SyntheticScene scene, Step st) {
        if (st.landmark == Landmark.CALIBRATION_EDGE) {
            return scene.stickerEdgePoints(16, 0, 7);
        }
        EyeLandmarks lm = scene.landmarksPx(st.eye);
        switch (st.landmark) {
            case PUPIL:
                return Collections.singletonList(lm.pupil);
            case UPPER_LID:
                return Collections.singletonList(lm.upperLid);
            case LOWER_LID:
                return Collections.singletonList(lm.lowerLid);
            case MEDIAL_CANTHUS:
                return Collections.singletonList(lm.medialCanthus);
            case LATERAL_CANTHUS:
                return Collections.singletonList(lm.lateralCanthus);
            default:
                throw new IllegalStateException("Unexpected step " + st);
        }
    }

    /** A real left-button press and release (no movement) on {@code c}, like a user's click. */
    private static void click(Component c, int x, int y) throws Exception {
        long now = System.currentTimeMillis();
        MouseEvent press = new MouseEvent(c, MouseEvent.MOUSE_PRESSED, now, InputEvent.BUTTON1_MASK, x, y, 1, false,
                MouseEvent.BUTTON1);
        MouseEvent release = new MouseEvent(c, MouseEvent.MOUSE_RELEASED, now + 60, InputEvent.BUTTON1_MASK, x, y, 1,
                false, MouseEvent.BUTTON1);
        edt(() -> {
            c.dispatchEvent(press);
            return null;
        });
        edt(() -> {
            c.dispatchEvent(release);
            return null;
        });
        Thread.sleep(120);
    }

    private static boolean markerOk(MainWindow w, Component c, ViewState v, Vec2 p) throws Exception {
        int[] s = v.toScreenPixel(p);
        BufferedImage bi = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        edt(() -> {
            // In one EDT task: a real mouse resting on the photo must not redirect the loupe.
            w.viewer().clearHover();
            c.paint(bi.createGraphics());
            return null;
        });
        return (bi.getRGB(s[0], s[1]) & 0xFFFFFF) == (OverlayPainter.SELECTED.getRGB() & 0xFFFFFF);
    }
}
