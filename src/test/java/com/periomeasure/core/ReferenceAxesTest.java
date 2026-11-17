package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ReferenceAxesTest {

    private static final double EPS = 1e-12;

    @Test
    void levelPupilsGiveImageAxesWithVPointingUp() {
        ReferenceAxes ax = ReferenceAxes.fromPupils(new Vec2(100, 200), new Vec2(300, 200));
        assertEquals(1, ax.u.x, EPS);
        assertEquals(0, ax.u.y, EPS);
        assertEquals(0, ax.v.x, EPS);
        assertEquals(-1, ax.v.y, EPS); // y grows downwards, so "up" is -y
        assertEquals(ReferenceAxes.Source.PUPILS, ax.source);
        assertFalse(ax.isMirrored());
        assertEquals(0, ax.tiltDegrees(), EPS);
    }

    @Test
    void rotatedPupilLine() {
        double phi = Math.toRadians(10);
        Vec2 od = new Vec2(500, 400);
        Vec2 os = od.plus(new Vec2(Math.cos(phi), Math.sin(phi)).times(250));
        ReferenceAxes ax = ReferenceAxes.fromPupils(od, os);
        assertEquals(Math.cos(phi), ax.u.x, EPS);
        assertEquals(Math.sin(phi), ax.u.y, EPS);
        assertEquals(Math.sin(phi), ax.v.x, EPS);
        assertEquals(-Math.cos(phi), ax.v.y, EPS);
        assertEquals(10, ax.tiltDegrees(), 1e-9);
    }

    @Test
    void axesAreOrthonormalWithUpwardVForAnyTilt() {
        Random rnd = new Random(1);
        for (int k = 0; k < 1000; k++) {
            double phi = Math.toRadians(-44 + 88 * rnd.nextDouble());
            boolean mirrored = rnd.nextBoolean();
            Vec2 od = new Vec2(1000 * rnd.nextDouble(), 1000 * rnd.nextDouble());
            Vec2 dir = new Vec2(Math.cos(phi), Math.sin(phi)).times(mirrored ? -1 : 1);
            ReferenceAxes ax = ReferenceAxes.fromPupils(od, od.plus(dir.times(60 + 300 * rnd.nextDouble())));
            assertEquals(1, ax.u.norm(), EPS);
            assertEquals(1, ax.v.norm(), EPS);
            assertEquals(0, ax.u.dot(ax.v), EPS);
            assertTrue(ax.v.y < 0);
            assertEquals(mirrored, ax.isMirrored());
        }
    }

    @Test
    void mirroredImageKeepsAnatomicalUButVStillPointsUp() {
        ReferenceAxes ax = ReferenceAxes.fromPupils(new Vec2(300, 200), new Vec2(100, 200));
        assertEquals(-1, ax.u.x, EPS);
        assertEquals(0, ax.v.x, EPS);
        assertEquals(-1, ax.v.y, EPS);
        assertTrue(ax.isMirrored());
    }

    @Test
    void coincidentPupilsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ReferenceAxes.fromPupils(new Vec2(5, 5), new Vec2(5, 5)));
    }

    @Test
    void axisSteeperThan45DegreesIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceAxes.fromPupils(new Vec2(100, 100), new Vec2(150, 300)));
    }

    @Test
    void userLineIsOrientedTowardsPositiveXWhicheverWayItWasDrawn() {
        ReferenceAxes a = ReferenceAxes.fromLine(new Vec2(10, 10), new Vec2(110, 20));
        ReferenceAxes b = ReferenceAxes.fromLine(new Vec2(110, 20), new Vec2(10, 10));
        assertEquals(a.u, b.u);
        assertEquals(a.v, b.v);
        assertTrue(a.u.x > 0);
        assertEquals(ReferenceAxes.Source.USER_LINE, a.source);
        assertFalse(a.isMirrored());
    }

    @Test
    void imageHorizontal() {
        ReferenceAxes ax = ReferenceAxes.imageHorizontal();
        assertEquals(new Vec2(1, 0), ax.u);
        assertEquals(new Vec2(0, -1), ax.v);
        assertEquals(ReferenceAxes.Source.IMAGE_HORIZONTAL, ax.source);
    }

    @Test
    void lateralDirectionIsMirroredBetweenEyes() {
        assertEquals(-1, Eye.OD.lateralSign());
        assertEquals(1, Eye.OS.lateralSign());
        ReferenceAxes ax = ReferenceAxes.imageHorizontal();
        assertEquals(new Vec2(-1, 0), ax.lateral(Eye.OD));
        assertEquals(new Vec2(1, 0), ax.lateral(Eye.OS));
    }
}
