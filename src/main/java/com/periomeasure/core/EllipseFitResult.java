package com.periomeasure.core;

/** Result of {@link EllipseFitter#fit}. All lengths in pixels. */
public final class EllipseFitResult {

    /** Geometrically refined ellipse. */
    public final Ellipse ellipse;
    /** RMS orthogonal distance of the input points to {@link #ellipse}. */
    public final double rmsResidualPx;
    /** RMS orthogonal distance to the algebraic (initial) ellipse, for diagnostics. */
    public final double algebraicRmsResidualPx;
    /** 1-sigma standard error of the full major axis (2a). NaN if fewer than 6 points. */
    public final double sigmaMajorAxisPx;
    /** 1-sigma standard error of the full minor axis (2b). NaN if fewer than 6 points. */
    public final double sigmaMinorAxisPx;
    /** Number of input points. */
    public final int pointCount;

    EllipseFitResult(Ellipse ellipse, double rmsResidualPx, double algebraicRmsResidualPx,
            double sigmaMajorAxisPx, double sigmaMinorAxisPx, int pointCount) {
        this.ellipse = ellipse;
        this.rmsResidualPx = rmsResidualPx;
        this.algebraicRmsResidualPx = algebraicRmsResidualPx;
        this.sigmaMajorAxisPx = sigmaMajorAxisPx;
        this.sigmaMinorAxisPx = sigmaMinorAxisPx;
        this.pointCount = pointCount;
    }
}
