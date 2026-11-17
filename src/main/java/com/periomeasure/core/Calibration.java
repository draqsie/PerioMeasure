package com.periomeasure.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Calibration from the circular reference sticker on the forehead.
 *
 * <p>An ellipse is fitted to the sticker edge points. In an oblique view a
 * circle is foreshortened only along one direction, so the MAJOR axis keeps
 * the true diameter: {@code mmPerPx = referenceDiameterMm / majorAxisPx}.
 */
public final class Calibration {

    public enum Method {
        /** Edge points found automatically from the image (Phase 4). */
        AUTO,
        /** Edge points clicked by the user. */
        MANUAL
    }

    public static final double DEFAULT_REFERENCE_DIAMETER_MM = 24.0;
    public static final int MIN_MANUAL_POINTS = 8;
    /** Six points leave one degree of freedom, so the axis uncertainty is defined. */
    public static final int MIN_AUTO_POINTS = 6;
    /** Below this minor/major ratio the photo is considered oblique. */
    public static final double OBLIQUE_RATIO_THRESHOLD = 0.97;
    /** Below this sticker diameter (px) the resolution is considered insufficient. */
    public static final double MIN_DIAMETER_PX = 150.0;

    public final Method method;
    public final double referenceDiameterMm;
    public final Ellipse ellipse;
    /** RMS orthogonal residual of the edge points (px); reported for manual calibration. */
    public final double rmsResidualPx;
    /** 1-sigma standard error of the major axis (px). */
    public final double sigmaMajorAxisPx;
    public final int pointCount;
    private final List<Warning> warnings;

    private Calibration(Method method, double referenceDiameterMm, EllipseFitResult fit) {
        this.method = method;
        this.referenceDiameterMm = referenceDiameterMm;
        this.ellipse = fit.ellipse;
        this.rmsResidualPx = fit.rmsResidualPx;
        this.sigmaMajorAxisPx = fit.sigmaMajorAxisPx;
        this.pointCount = fit.pointCount;
        List<Warning> w = new ArrayList<>();
        if (ellipse.axisRatio() < OBLIQUE_RATIO_THRESHOLD) {
            w.add(new Warning(WarningCode.OBLIQUE_VIEW, null, ellipse.axisRatio()));
        }
        if (ellipse.majorAxis() < MIN_DIAMETER_PX) {
            w.add(new Warning(WarningCode.LOW_RESOLUTION, null, ellipse.majorAxis()));
        }
        this.warnings = Collections.unmodifiableList(w);
    }

    /**
     * @param points              sticker edge points (image coordinates)
     * @param method              how the points were obtained
     * @param referenceDiameterMm true sticker diameter, normally {@value #DEFAULT_REFERENCE_DIAMETER_MM}
     * @throws IllegalArgumentException for too few points or an invalid diameter
     * @throws EllipseFitException      if the points do not determine an ellipse
     */
    public static Calibration fromEdgePoints(List<Vec2> points, Method method, double referenceDiameterMm) {
        Objects.requireNonNull(points, "points");
        Objects.requireNonNull(method, "method");
        if (Double.isNaN(referenceDiameterMm) || Double.isInfinite(referenceDiameterMm) || referenceDiameterMm <= 0) {
            throw new IllegalArgumentException("Reference diameter must be positive: " + referenceDiameterMm);
        }
        int min = method == Method.MANUAL ? MIN_MANUAL_POINTS : MIN_AUTO_POINTS;
        if (points.size() < min) {
            throw new IllegalArgumentException(method + " calibration needs at least " + min
                    + " edge points, got " + points.size());
        }
        return new Calibration(method, referenceDiameterMm, EllipseFitter.fit(points));
    }

    public double majorAxisPx() {
        return ellipse.majorAxis();
    }

    public double minorAxisPx() {
        return ellipse.minorAxis();
    }

    /** @return minor / major axis ratio, in (0, 1] */
    public double axisRatio() {
        return ellipse.axisRatio();
    }

    public double mmPerPx() {
        return referenceDiameterMm / ellipse.majorAxis();
    }

    /** @return sigma(major axis) / major axis; equals the relative uncertainty of mmPerPx */
    public double relativeUncertainty() {
        return sigmaMajorAxisPx / ellipse.majorAxis();
    }

    public Scale scale() {
        return new Scale(mmPerPx(), relativeUncertainty());
    }

    public List<Warning> warnings() {
        return warnings;
    }
}
