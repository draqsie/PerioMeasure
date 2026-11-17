package com.periomeasure.core;

/**
 * The single definition of the pixel coordinate convention used everywhere
 * in PerioMeasure.
 *
 * <p>Continuous image coordinates: x to the right, y DOWN. The pixel with
 * integer index (i, j) covers the square [i, i+1) x [j, j+1), so its centre
 * is at (i + 0.5, j + 0.5). This matches ImageJ's sub-pixel (float) ROI
 * coordinates and Java2D's rasterisation rule.
 */
public final class PixelConvention {

    /** Offset from a pixel's corner (integer index) to its centre. */
    public static final double CENTER_OFFSET = 0.5;

    private PixelConvention() {
    }

    /** @return continuous coordinate of the centre of pixel {@code index} */
    public static double center(int index) {
        return index + CENTER_OFFSET;
    }

    /** @return continuous coordinates of the centre of pixel (i, j) */
    public static Vec2 centerOf(int i, int j) {
        return new Vec2(center(i), center(j));
    }

    /** @return index of the pixel that covers continuous coordinate {@code coordinate} */
    public static int indexOf(double coordinate) {
        return (int) Math.floor(coordinate);
    }
}
