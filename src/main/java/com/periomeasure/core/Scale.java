package com.periomeasure.core;

/**
 * Image scale used to convert pixel measurements to millimetres.
 * {@code relativeSigma} is the relative 1-sigma uncertainty of the scale
 * (from the calibration fit); it is the same for every length in the image.
 */
public final class Scale {

    public final double mmPerPx;
    public final double relativeSigma;

    public Scale(double mmPerPx, double relativeSigma) {
        if (Double.isNaN(mmPerPx) || Double.isInfinite(mmPerPx) || mmPerPx <= 0) {
            throw new IllegalArgumentException("mm per pixel must be positive and finite: " + mmPerPx);
        }
        if (Double.isNaN(relativeSigma) || Double.isInfinite(relativeSigma) || relativeSigma < 0) {
            throw new IllegalArgumentException("Relative sigma must be finite and >= 0: " + relativeSigma);
        }
        this.mmPerPx = mmPerPx;
        this.relativeSigma = relativeSigma;
    }
}
