package com.periomeasure.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Vec2;

class ZoomPlanTest {

    @Test
    void sourceRectangleIsCentredOnTheClick() {
        IntRect r = ZoomPlan.sourceRect(new Vec2(1000.5, 800.5), 6, 1200, 900, 4000, 3000);
        assertEquals(200, r.width);
        assertEquals(150, r.height);
        assertEquals(900, r.x);
        assertEquals(725, r.y);
    }

    @Test
    void sourceRectangleIsClampedInsideTheImage() {
        IntRect topLeft = ZoomPlan.sourceRect(new Vec2(5, 5), 6, 1200, 900, 4000, 3000);
        assertEquals(0, topLeft.x);
        assertEquals(0, topLeft.y);
        IntRect bottomRight = ZoomPlan.sourceRect(new Vec2(3999, 2999), 6, 1200, 900, 4000, 3000);
        assertEquals(3800, bottomRight.x);
        assertEquals(2850, bottomRight.y);
    }

    @Test
    void neverLargerThanTheImage() {
        IntRect r = ZoomPlan.sourceRect(new Vec2(50, 40), 4, 1200, 900, 100, 80);
        assertEquals(0, r.x);
        assertEquals(0, r.y);
        assertEquals(100, r.width);
        assertEquals(80, r.height);
    }
}
