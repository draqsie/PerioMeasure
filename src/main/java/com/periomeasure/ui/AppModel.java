package com.periomeasure.ui;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.periomeasure.Version;
import com.periomeasure.core.Vec2;
import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;
import com.periomeasure.io.CloudFolders;
import com.periomeasure.io.FileHash;
import com.periomeasure.io.MeasurementRecord;
import com.periomeasure.io.ResultCsv;
import com.periomeasure.io.Settings;
import com.periomeasure.session.ClickPolicy;
import com.periomeasure.session.Handle;
import com.periomeasure.session.MeasurementSession;
import com.periomeasure.session.SessionSetup;
import com.periomeasure.synthetic.GroundTruth;

/**
 * State of the single-window application, without any Swing: which phase
 * it is in, the loaded image, the measurement session, the selected point,
 * dragging and loupe refinement, finishing and saving. The window only
 * forwards mouse/button events here and renders the result, so the whole
 * workflow is unit-tested.
 *
 * <p>Workflow: {@code NO_IMAGE -> SETUP -> MEASURING -> RESULTS}. One click
 * places a point; a step that is then full advances by itself. The last
 * placed (or dragged) point is "selected" and can be fine-tuned in the
 * loupe at sub-pixel resolution.
 */
public final class AppModel {

    public enum Phase {
        NO_IMAGE,
        SETUP,
        MEASURING,
        RESULTS
    }

    public enum PlaceResult {
        /** Point placed; the step continues (or it was the last step). */
        PLACED,
        /** Point placed and the session moved on to the next step. */
        ADVANCED,
        /** The step already holds its maximum number of points. */
        FULL
    }

    /** Result of {@link #finish}. */
    public static final class Outcome {
        public final boolean ok;
        public final MeasurementRecord record;
        public final ResultCsv.Written files;
        public final String error;

        Outcome(MeasurementRecord record, ResultCsv.Written files, String error) {
            this.ok = error == null;
            this.record = record;
            this.files = files;
            this.error = error;
        }
    }

    private Settings settings;
    private final Map<String, String> env;
    private final File localOutputRoot;

    private Phase phase = Phase.NO_IMAGE;
    private File photo;
    private File outputOverride;
    private int width;
    private int height;
    private List<Warning> imageWarnings = Collections.emptyList();
    private GroundTruth truth;
    private MeasurementSession session;
    private Handle selected;
    private boolean dragging;
    private Outcome lastOutcome;

    /**
     * @param localOutputRoot local folder used instead of a cloud-synchronised default output folder
     */
    public AppModel(Settings settings, Map<String, String> env, File localOutputRoot) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.env = Objects.requireNonNull(env, "env");
        this.localOutputRoot = Objects.requireNonNull(localOutputRoot, "localOutputRoot");
    }

    // ------------------------------------------------------------------ image and setup

    /**
     * @param photo    the photograph file, or null (demo / unsaved image)
     * @param warnings image-level warnings found while loading
     * @param truth    true values (demo images only), else null
     */
    public void imageLoaded(File photo, int width, int height, List<Warning> warnings, GroundTruth truth) {
        imageLoaded(photo, width, height, warnings, truth, null);
    }

    /** @param outputOverride folder used for this image only (demo faces), or null */
    public void imageLoaded(File photo, int width, int height, List<Warning> warnings, GroundTruth truth,
            File outputOverride) {
        this.outputOverride = outputOverride;
        this.photo = photo;
        this.width = width;
        this.height = height;
        this.imageWarnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        this.truth = truth;
        newMeasurement();
    }

    public void start(SessionSetup setup) {
        if (phase == Phase.NO_IMAGE) {
            throw new IllegalStateException("No image loaded");
        }
        session = new MeasurementSession(setup, settings.referenceDiameterMm);
        selected = null;
        dragging = false;
        phase = Phase.MEASURING;
    }

    /** Back to the setup form for the same image. */
    public void newMeasurement() {
        session = null;
        selected = null;
        dragging = false;
        lastOutcome = null;
        phase = photoOrImageLoaded() ? Phase.SETUP : Phase.NO_IMAGE;
    }

    private boolean photoOrImageLoaded() {
        return width > 0 && height > 0;
    }

    public void setSettings(Settings s) {
        this.settings = Objects.requireNonNull(s, "settings");
    }

    public Settings settings() {
        return settings;
    }

    public Phase phase() {
        return phase;
    }

    public File photo() {
        return photo;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<Warning> imageWarnings() {
        return imageWarnings;
    }

    public GroundTruth truth() {
        return truth;
    }

    public MeasurementSession session() {
        return session;
    }

    public Handle selected() {
        return selected;
    }

    public Outcome lastOutcome() {
        return lastOutcome;
    }

    // ------------------------------------------------------------------ points

    private boolean measuring() {
        return phase == Phase.MEASURING && session != null;
    }

    private Vec2 clamp(Vec2 p) {
        return new Vec2(Math.max(0, Math.min(width, p.x)), Math.max(0, Math.min(height, p.y)));
    }

    /** Places a point in the current step; advances if the step is then full. */
    public PlaceResult place(Vec2 p) {
        if (!measuring()) {
            return PlaceResult.FULL;
        }
        Vec2 q = clamp(p);
        int step = session.currentIndex();
        if (!session.place(q)) {
            return PlaceResult.FULL;
        }
        int n = session.points(step).size();
        selected = new Handle(step, n - 1);
        if (ClickPolicy.advanceAfterPlacing(session.current().landmark, n) && !session.isLast()) {
            session.next();
            return PlaceResult.ADVANCED;
        }
        return PlaceResult.PLACED;
    }

    /** @return the point within {@code radius} image px under the cursor, or null */
    public Handle hit(Vec2 p, double radius) {
        return measuring() ? session.hit(p, radius) : null;
    }

    public void startDrag(Handle h) {
        if (!measuring() || h == null) {
            return;
        }
        session.beginDrag(h);
        selected = h;
        dragging = true;
    }

    public void dragTo(Vec2 p) {
        if (dragging && measuring()) {
            session.dragTo(selected, clamp(p));
        }
    }

    public void endDrag() {
        dragging = false;
    }

    public boolean isDragging() {
        return dragging;
    }

    /** Starts a loupe refinement of the selected point (one undo step). */
    public void beginRefine() {
        if (measuring() && selected != null) {
            session.beginDrag(selected);
            dragging = true;
        }
    }

    public void refineTo(Vec2 p) {
        dragTo(p);
    }

    /**
     * @param hover image point under the mouse in the main view, or null
     * @return what the loupe should show: the hovered point, else the selected point, else null
     */
    public Vec2 loupeCenter(Vec2 hover) {
        if (hover != null) {
            return hover;
        }
        if (measuring() && selected != null) {
            return session.points(selected.stepIndex).get(selected.pointIndex);
        }
        return null;
    }

    // ------------------------------------------------------------------ navigation

    public void undo() {
        if (measuring() && session.undo()) {
            int n = session.points(session.currentIndex()).size();
            selected = n > 0 ? new Handle(session.currentIndex(), n - 1) : null;
            dragging = false;
        }
    }

    public void next() {
        if (measuring() && session.canAdvance() && !session.isLast()) {
            session.next();
        }
    }

    public void previous() {
        if (measuring()) {
            session.previous();
        }
    }

    public void skip() {
        if (measuring() && session.current().optional) {
            session.skip();
            selected = null;
        }
    }

    // ------------------------------------------------------------------ output

    /** Explicit folder from Settings; else next to the photo unless that is cloud-synchronised. */
    public File outputDir() {
        if (outputOverride != null) {
            return outputOverride;
        }
        if (!settings.outputDir.isEmpty()) {
            return new File(settings.outputDir);
        }
        File dflt = settings.outputDirFor(photo);
        return CloudFolders.provider(dflt, env) != null ? localOutputRoot : dflt;
    }

    public List<Warning> outputWarnings() {
        List<Warning> w = new ArrayList<>();
        String provider = CloudFolders.provider(outputDir(), env);
        if (provider != null) {
            w.add(new Warning(WarningCode.CLOUD_FOLDER_OUTPUT, null, Double.NaN, provider));
        }
        return w;
    }

    /** Computes, saves the CSV and moves to the results; on error stays in MEASURING. */
    public Outcome finish(LocalDateTime now) {
        if (!measuring()) {
            return new Outcome(null, null, "No measurement in progress");
        }
        List<Warning> warnings = new ArrayList<>(imageWarnings);
        warnings.addAll(outputWarnings());
        MeasurementRecord record;
        try {
            record = session.compute(Version.get(), now, FileHash.fileNameHash(photo), width, height, warnings);
        } catch (RuntimeException e) {
            return new Outcome(null, null, String.valueOf(e.getMessage()));
        }
        ResultCsv.Written files;
        try {
            files = ResultCsv.write(record, outputDir(), settings.decimalSeparator);
        } catch (IOException | RuntimeException e) {
            return new Outcome(record, null, String.valueOf(e.getMessage()));
        }
        lastOutcome = new Outcome(record, files, null);
        phase = Phase.RESULTS;
        dragging = false;
        return lastOutcome;
    }
}
