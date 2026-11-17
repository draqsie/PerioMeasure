package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Vec2Test {

    private static final double EPS = 1e-12;

    @Test
    void arithmetic() {
        Vec2 a = new Vec2(3, 4);
        Vec2 b = new Vec2(-1, 2.5);
        assertEquals(new Vec2(2, 6.5), a.plus(b));
        assertEquals(new Vec2(4, 1.5), a.minus(b));
        assertEquals(new Vec2(6, 8), a.times(2));
        assertEquals(-3 + 10, a.dot(b), EPS);
        assertEquals(3 * 2.5 - 4 * -1, a.cross(b), EPS);
        assertEquals(5, a.norm(), EPS);
        assertEquals(Math.hypot(4, 1.5), a.distanceTo(b), EPS);
    }

    @Test
    void normalizedHasUnitLengthAndSameDirection() {
        Vec2 n = new Vec2(3, -4).normalized();
        assertEquals(1, n.norm(), EPS);
        assertEquals(0.6, n.x, EPS);
        assertEquals(-0.8, n.y, EPS);
    }

    @Test
    void normalizingZeroVectorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Vec2(0, 0).normalized());
    }

    @Test
    void nonFiniteCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Vec2(Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new Vec2(0, Double.POSITIVE_INFINITY));
    }

    @Test
    void valueEquality() {
        assertEquals(new Vec2(1.5, -2), new Vec2(1.5, -2));
        assertEquals(new Vec2(1.5, -2).hashCode(), new Vec2(1.5, -2).hashCode());
        assertNotEquals(new Vec2(1.5, -2), new Vec2(1.5, 2));
    }

    @Test
    void negativeZeroEqualsZero() {
        Vec2 a = new Vec2(1, 0).times(-1); // (-1, -0.0)
        assertEquals(new Vec2(-1, 0), a);
        assertEquals(new Vec2(-1, 0).hashCode(), a.hashCode());
    }

    @Test
    void toStringUsesDotDecimalRegardlessOfDefaultLocale() {
        // Tests run with tr_TR, where the default decimal separator is a comma.
        assertEquals("(1.5, -2.25)", new Vec2(1.5, -2.25).toString());
    }
}
