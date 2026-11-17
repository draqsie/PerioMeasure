package com.periomeasure.core;

import java.util.Locale;
import java.util.Objects;

/**
 * A measured value with its 1-sigma uncertainty split into two components:
 * <ul>
 *   <li>{@code sigmaClick}: from the 1-pixel click precision,</li>
 *   <li>{@code sigmaCal}: from the calibration (scale) uncertainty.</li>
 * </ul>
 * The UI shows only {@link #sigmaTotal()}; the CSV keeps all three.
 */
public final class Quantity {

    public final double value;
    public final double sigmaClick;
    public final double sigmaCal;
    public final Unit unit;

    public Quantity(double value, double sigmaClick, double sigmaCal, Unit unit) {
        this.value = value;
        this.sigmaClick = sigmaClick;
        this.sigmaCal = sigmaCal;
        this.unit = Objects.requireNonNull(unit, "unit");
    }

    /** @return sqrt(sigmaClick^2 + sigmaCal^2) */
    public double sigmaTotal() {
        return Math.hypot(sigmaClick, sigmaCal);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.6f +/- %.6f %s", value, sigmaTotal(), unit);
    }
}
