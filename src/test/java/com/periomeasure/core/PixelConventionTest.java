package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pixel (i, j) covers [i, i+1) x [j, j+1); its centre is (i + 0.5, j + 0.5).
 * This is the convention of ImageJ's sub-pixel (float) ROI coordinates.
 */
class PixelConventionTest {

    @Test
    void pixelCentreIsHalfAPixelFromTheCorner() {
        assertEquals(0.5, PixelConvention.center(0), 0);
        assertEquals(9.5, PixelConvention.center(9), 0);
        assertEquals(new Vec2(3.5, 7.5), PixelConvention.centerOf(3, 7));
    }

    @Test
    void continuousCoordinateMapsToTheCoveringPixel() {
        assertEquals(0, PixelConvention.indexOf(0.0));
        assertEquals(0, PixelConvention.indexOf(0.999999));
        assertEquals(1, PixelConvention.indexOf(1.0));
        assertEquals(-1, PixelConvention.indexOf(-0.1));
    }

    @Test
    void centreRoundTripsToItsOwnIndex() {
        for (int i = -5; i < 5000; i++) {
            assertEquals(i, PixelConvention.indexOf(PixelConvention.center(i)));
        }
    }
}
