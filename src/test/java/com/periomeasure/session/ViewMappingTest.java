package com.periomeasure.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Vec2;

/**
 * Screen pixel sx at magnification m, with source rectangle origin srcX,
 * shows image coordinates [srcX + sx/m, srcX + (sx+1)/m). A click is mapped
 * to the CENTRE of that interval; a marker is drawn on the screen pixel that
 * contains the point. Clicking a screen pixel must therefore draw the marker
 * on exactly that pixel.
 */
class ViewMappingTest {

    @Test
    void clickMapsToTheCentreOfTheScreenPixel() {
        assertEquals(new Vec2(10.5, 20.5), ViewMapping.screenToImage(10, 20, 0, 0, 1));
        assertEquals(new Vec2(100 + 13.5 / 4, 50 + 7.5 / 4), ViewMapping.screenToImage(13, 7, 100, 50, 4));
    }

    @Test
    void markerIsDrawnOnTheClickedScreenPixelAtAnyMagnification() {
        double[] mags = {0.125, 0.25, 1.0 / 3, 0.5, 0.75, 1, 1.5, 2, 3, 4, 6, 8};
        for (double m : mags) {
            for (int sx = 0; sx < 400; sx += 7) {
                for (int sy = 0; sy < 300; sy += 11) {
                    Vec2 p = ViewMapping.screenToImage(sx, sy, 37, 81, m);
                    int[] back = ViewMapping.imageToScreenPixel(p, 37, 81, m);
                    assertEquals(sx, back[0], "mag " + m);
                    assertEquals(sy, back[1], "mag " + m);
                }
            }
        }
    }

    @Test
    void imagePixelCentreIsDrawnInTheMiddleOfItsMagnifiedBlock() {
        // Image pixel (5, 9) at 6x with origin (2, 3): screen block x in [18, 24), y in [36, 42).
        int[] s = ViewMapping.imageToScreenPixel(new Vec2(5.5, 9.5), 2, 3, 6);
        assertEquals(21, s[0]);
        assertEquals(39, s[1]);
    }

    @Test
    void screenDistanceScalesWithMagnification() {
        assertEquals(12, ViewMapping.screenDistance(new Vec2(0, 0), new Vec2(3, 4), 2.4), 1e-12);
    }
}
