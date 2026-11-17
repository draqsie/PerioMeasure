package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class PolygonsTest {

    private static final List<Vec2> SQUARE = Arrays.asList(
            new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 10), new Vec2(0, 10));

    @Test
    void areaOfSquareAndTriangle() {
        assertEquals(100, Polygons.area(SQUARE), 1e-12);
        assertEquals(6, Polygons.area(Arrays.asList(new Vec2(0, 0), new Vec2(4, 0), new Vec2(0, 3))), 1e-12);
    }

    @Test
    void areaDoesNotDependOnOrientationOrStartVertex() {
        List<Vec2> reversed = new ArrayList<>(SQUARE);
        Collections.reverse(reversed);
        assertEquals(100, Polygons.area(reversed), 1e-12);
        List<Vec2> rotated = new ArrayList<>(SQUARE);
        Collections.rotate(rotated, 1);
        assertEquals(100, Polygons.area(rotated), 1e-12);
    }

    @Test
    void areaOfFarAwayPolygonIsAccurate() {
        List<Vec2> far = new ArrayList<>();
        for (Vec2 p : SQUARE) {
            far.add(p.plus(new Vec2(1e6, 2e6)));
        }
        assertEquals(100, Polygons.area(far), 1e-6);
    }

    @Test
    void perimeterOfSquare() {
        assertEquals(40, Polygons.perimeter(SQUARE), 1e-12);
    }

    @Test
    void fewerThanThreeVerticesIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> Polygons.area(Arrays.asList(new Vec2(0, 0), new Vec2(1, 1))));
    }
}
