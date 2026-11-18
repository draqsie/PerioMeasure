package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Vec2;
import com.periomeasure.session.ViewMapping;

class ViewStateTest {

    @Test
    void fitShowsTheWholeImageCentred() {
        ViewState v = ViewState.fit(2000, 1000, 1000, 800);
        assertEquals(0.5, v.scale, 1e-12);
        assertEquals(0, v.originX, 1e-12);
        assertEquals(-300, v.originY, 1e-12); // 150 px empty band above the image
        assertEquals(1000, v.toScreenX(2000), 1e-12);
        assertEquals(650, v.toScreenY(1000), 1e-12);
    }

    @Test
    void clickMappingUsesThePixelCentreConvention() {
        ViewState v = new ViewState(10.25, 20.5, 4);
        assertEquals(ViewMapping.screenToImage(7, 9, 10.25, 20.5, 4), v.screenToImage(7, 9));
        assertEquals(new Vec2(10.25 + 7.5 / 4, 20.5 + 9.5 / 4), v.screenToImage(7, 9));
    }

    @Test
    void zoomKeepsThePointUnderTheCursorFixed() {
        Random rnd = new Random(3);
        ViewState v = ViewState.fit(4000, 3000, 1200, 900);
        for (int k = 0; k < 200; k++) {
            int sx = rnd.nextInt(1200);
            int sy = rnd.nextInt(900);
            Vec2 before = v.screenToImage(sx, sy);
            v = v.zoomAt(rnd.nextBoolean() ? 1.25 : 0.8, sx, sy);
            Vec2 after = v.screenToImage(sx, sy);
            assertEquals(before.x, after.x, 1e-9);
            assertEquals(before.y, after.y, 1e-9);
        }
    }

    @Test
    void zoomIsClamped() {
        ViewState v = new ViewState(0, 0, 1);
        for (int i = 0; i < 100; i++) {
            v = v.zoomAt(2, 0, 0);
        }
        assertEquals(ViewState.MAX_SCALE, v.scale, 1e-12);
        for (int i = 0; i < 200; i++) {
            v = v.zoomAt(0.5, 0, 0);
        }
        assertEquals(ViewState.MIN_SCALE, v.scale, 1e-12);
    }

    @Test
    void panningMovesTheImageWithTheMouse() {
        ViewState v = new ViewState(100, 50, 2);
        Vec2 p = v.screenToImage(300, 200);
        ViewState moved = v.pan(40, -10);
        assertEquals(p, moved.screenToImage(340, 190));
    }

    @Test
    void centredViewPutsAPointInTheMiddle() {
        ViewState v = ViewState.centeredOn(new Vec2(123.25, 77.75), 6, 240, 180);
        assertEquals(120, v.toScreenX(123.25), 1e-9);
        assertEquals(90, v.toScreenY(77.75), 1e-9);
        assertTrue(v.screenToImage(120, 90).distanceTo(new Vec2(123.25, 77.75)) <= 0.5 / 6 * Math.sqrt(2));
    }
}
