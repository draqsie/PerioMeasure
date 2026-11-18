package com.periomeasure.session;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.periomeasure.core.BilateralMeasurements;
import com.periomeasure.core.Calibration;
import com.periomeasure.core.EllipseFitException;
import com.periomeasure.core.EllipseFitResult;
import com.periomeasure.core.EllipseFitter;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.EyeMeasurements;
import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.MeasurementEngine;
import com.periomeasure.core.ReferenceAxes;
import com.periomeasure.core.Scale;
import com.periomeasure.core.Vec2;
import com.periomeasure.core.Warning;
import com.periomeasure.io.MeasurementRecord;

/**
 * State of one measurement session, independent of ImageJ: the steps, the
 * points placed in each step (image coordinates, sub-pixel), navigation,
 * dragging and undo, and the final computation.
 *
 * <p>Undo reverts point edits (place, drag, skip) and returns to the step
 * in which the edit was made; plain navigation is not an edit.
 */
public final class MeasurementSession {

    private final SessionSetup setup;
    private final double referenceDiameterMm;
    private final List<Step> steps;
    private List<List<Vec2>> points;
    private int index;
    private final Deque<Snapshot> undo = new ArrayDeque<>();

    private static final class Snapshot {
        final List<List<Vec2>> points;
        final int index;

        Snapshot(List<List<Vec2>> points, int index) {
            this.points = points;
            this.index = index;
        }
    }

    public MeasurementSession(SessionSetup setup, double referenceDiameterMm) {
        this.setup = Objects.requireNonNull(setup, "setup");
        this.referenceDiameterMm = referenceDiameterMm;
        this.steps = StepPlan.build(setup);
        this.points = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            points.add(new ArrayList<Vec2>());
        }
    }

    public SessionSetup setup() {
        return setup;
    }

    public double referenceDiameterMm() {
        return referenceDiameterMm;
    }

    public List<Step> steps() {
        return steps;
    }

    public int currentIndex() {
        return index;
    }

    public Step current() {
        return steps.get(index);
    }

    /** @return a copy of the points of step {@code stepIndex} */
    public List<Vec2> points(int stepIndex) {
        return Collections.unmodifiableList(new ArrayList<>(points.get(stepIndex)));
    }

    /** A step is satisfied with at least its minimum points, or with none if optional. */
    public boolean isSatisfied(int stepIndex) {
        Step s = steps.get(stepIndex);
        int n = points.get(stepIndex).size();
        return n >= s.landmark.minPoints || (s.optional && n == 0);
    }

    public boolean canAdvance() {
        return isSatisfied(index);
    }

    public boolean isLast() {
        return index == steps.size() - 1;
    }

    public boolean canFinish() {
        for (int i = 0; i < steps.size(); i++) {
            if (!isSatisfied(i)) {
                return false;
            }
        }
        return true;
    }

    /** @throws IllegalStateException at the last step or if the current step is not satisfied */
    public void next() {
        if (isLast()) {
            throw new IllegalStateException("Already at the last step");
        }
        if (!canAdvance()) {
            throw new IllegalStateException("Step " + current() + " needs at least "
                    + current().landmark.minPoints + " point(s)");
        }
        index++;
    }

    public void previous() {
        if (index > 0) {
            index--;
        }
    }

    /** Clears an optional step and moves on. @throws IllegalStateException for a required step */
    public void skip() {
        if (!current().optional) {
            throw new IllegalStateException("Step " + current() + " is required");
        }
        pushUndo();
        points.get(index).clear();
        if (!isLast()) {
            index++;
        }
    }

    /**
     * Places a point in the current step: replaces it for single-point
     * landmarks, appends it for multi-point ones.
     *
     * @return false if the step already holds its maximum number of points
     */
    public boolean place(Vec2 p) {
        Objects.requireNonNull(p, "point");
        List<Vec2> list = points.get(index);
        Landmark l = current().landmark;
        if (l.maxPoints == 1) {
            pushUndo();
            list.clear();
            list.add(p);
            return true;
        }
        if (list.size() >= l.maxPoints) {
            return false;
        }
        pushUndo();
        list.add(p);
        return true;
    }

    /**
     * @return the nearest point within {@code radius} (image px), preferring
     *         points of the current step; null if none
     */
    public Handle hit(Vec2 p, double radius) {
        Handle h = nearest(p, radius, index, index);
        return h != null ? h : nearest(p, radius, 0, steps.size() - 1);
    }

    private Handle nearest(Vec2 p, double radius, int from, int to) {
        Handle best = null;
        double bestD = radius;
        for (int s = from; s <= to; s++) {
            List<Vec2> list = points.get(s);
            for (int i = 0; i < list.size(); i++) {
                double d = list.get(i).distanceTo(p);
                if (d <= bestD) {
                    bestD = d;
                    best = new Handle(s, i);
                }
            }
        }
        return best;
    }

    /** Records the state before a drag so that the whole drag is one undo step. */
    public void beginDrag(Handle h) {
        checkHandle(h);
        pushUndo();
    }

    public void dragTo(Handle h, Vec2 p) {
        checkHandle(h);
        points.get(h.stepIndex).set(h.pointIndex, Objects.requireNonNull(p, "point"));
    }

    private void checkHandle(Handle h) {
        if (h.stepIndex < 0 || h.stepIndex >= steps.size() || h.pointIndex < 0
                || h.pointIndex >= points.get(h.stepIndex).size()) {
            throw new IllegalArgumentException("No such point");
        }
    }

    /** @return false if there is nothing to undo */
    public boolean undo() {
        Snapshot s = undo.pollFirst();
        if (s == null) {
            return false;
        }
        points = s.points;
        index = s.index;
        return true;
    }

    private void pushUndo() {
        List<List<Vec2>> copy = new ArrayList<>();
        for (List<Vec2> l : points) {
            copy.add(new ArrayList<>(l));
        }
        undo.addFirst(new Snapshot(copy, index));
    }

    // ------------------------------------------------------------------ derived views

    private int stepIndex(Landmark l, Eye eye) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).landmark == l && steps.get(i).eye == eye) {
                return i;
            }
        }
        return -1;
    }

    private Vec2 first(Landmark l, Eye eye) {
        int i = stepIndex(l, eye);
        return i < 0 || points.get(i).isEmpty() ? null : points.get(i).get(0);
    }

    /** @return live ellipse fit of the sticker points (5 or more), or null */
    public EllipseFitResult liveFit() {
        List<Vec2> cal = points.get(stepIndex(Landmark.CALIBRATION_EDGE, null));
        if (cal.size() < EllipseFitter.MIN_POINTS) {
            return null;
        }
        try {
            return EllipseFitter.fit(cal);
        } catch (EllipseFitException | IllegalArgumentException e) {
            return null;
        }
    }

    public Vec2 pupil(Eye eye) {
        return first(Landmark.PUPIL, eye);
    }

    /** @return the measurement axes as far as they are known now (for drawing guides), or null */
    public ReferenceAxes previewAxes() {
        try {
            if (setup.eyes == EyeSelection.BOTH) {
                Vec2 od = pupil(Eye.OD);
                Vec2 os = pupil(Eye.OS);
                return od == null || os == null ? null : ReferenceAxes.fromPupils(od, os);
            }
            if (setup.axisMode == AxisMode.USER_LINE) {
                List<Vec2> line = points.get(stepIndex(Landmark.REFERENCE_LINE, null));
                return line.size() < 2 ? null : ReferenceAxes.fromLine(line.get(0), line.get(1));
            }
            return ReferenceAxes.imageHorizontal();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    EyeLandmarks landmarks(Eye eye) {
        EyeLandmarks.Builder b = EyeLandmarks.builder()
                .pupil(first(Landmark.PUPIL, eye))
                .upperLid(first(Landmark.UPPER_LID, eye))
                .lowerLid(first(Landmark.LOWER_LID, eye))
                .medialCanthus(first(Landmark.MEDIAL_CANTHUS, eye))
                .lateralCanthus(first(Landmark.LATERAL_CANTHUS, eye))
                .upperLidCrease(first(Landmark.UPPER_LID_CREASE, eye))
                .browLowerBorder(first(Landmark.BROW_LOWER_BORDER, eye))
                .limbusUpper(first(Landmark.LIMBUS_UPPER, eye))
                .limbusLower(first(Landmark.LIMBUS_LOWER, eye));
        int c = stepIndex(Landmark.CONTOUR, eye);
        if (c >= 0 && !points.get(c).isEmpty()) {
            b.contour(new ArrayList<>(points.get(c)));
        }
        return b.build();
    }

    // ------------------------------------------------------------------ result

    /**
     * Computes calibration and measurements.
     *
     * @throws IllegalStateException    if a step is not satisfied
     * @throws EllipseFitException      if the sticker points do not determine an ellipse
     * @throws IllegalArgumentException for impossible geometry (e.g. axis steeper than 45 degrees)
     */
    public MeasurementRecord compute(String version, LocalDateTime now, String fileHash, int width, int height,
            List<Warning> imageWarnings) {
        if (!canFinish()) {
            throw new IllegalStateException("Not all steps are complete");
        }
        Calibration cal = Calibration.fromEdgePoints(points.get(stepIndex(Landmark.CALIBRATION_EDGE, null)),
                Calibration.Method.MANUAL, referenceDiameterMm);
        Scale scale = cal.scale();
        Map<Eye, EyeMeasurements> per = new EnumMap<>(Eye.class);
        BilateralMeasurements both = null;
        ReferenceAxes axes;
        if (setup.eyes == EyeSelection.BOTH) {
            both = MeasurementEngine.measureBoth(landmarks(Eye.OD), landmarks(Eye.OS), scale);
            axes = both.axes;
            per.put(Eye.OD, both.od);
            per.put(Eye.OS, both.os);
        } else {
            if (setup.axisMode == AxisMode.USER_LINE) {
                List<Vec2> line = points.get(stepIndex(Landmark.REFERENCE_LINE, null));
                axes = ReferenceAxes.fromLine(line.get(0), line.get(1));
            } else {
                axes = ReferenceAxes.imageHorizontal();
            }
            Eye eye = setup.eyes.eyes().get(0);
            per.put(eye, MeasurementEngine.measureEye(eye, landmarks(eye), axes, scale));
        }
        return new MeasurementRecord(version, now, setup.patientId, setup.timing, setup.photoDate, fileHash, width,
                height, cal, axes, setup.eyes, per, both, imageWarnings);
    }
}
