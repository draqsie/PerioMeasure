package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class EllipseTest {

    private static final double EPS = 1e-9;

    @Test
    void semiMajorIsAlwaysTheLargerAxisAndAngleIsInZeroToPi() {
        Ellipse e = new Ellipse(10, 20, 5, 8, 0.2);
        assertEquals(8, e.a, 0);
        assertEquals(5, e.b, 0);
        assertEquals(0.2 + Math.PI / 2, e.theta, EPS);

        Ellipse f = new Ellipse(0, 0, 8, 5, -0.3);
        assertEquals(Math.PI - 0.3, f.theta, EPS);
        Ellipse g = new Ellipse(0, 0, 8, 5, 3 * Math.PI + 0.1);
        assertEquals(0.1, g.theta, EPS);
    }

    @Test
    void swappingAxesDescribesTheSameCurve() {
        Ellipse e = new Ellipse(10, 20, 5, 8, 0.2);
        for (int k = 0; k < 64; k++) {
            double t = 2 * Math.PI * k / 64;
            Vec2 raw = new Vec2(10 + 5 * Math.cos(t) * Math.cos(0.2) - 8 * Math.sin(t) * Math.sin(0.2),
                    20 + 5 * Math.cos(t) * Math.sin(0.2) + 8 * Math.sin(t) * Math.cos(0.2));
            assertEquals(0, e.signedDistance(raw), 1e-9);
        }
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Ellipse(0, 0, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Ellipse(0, 0, 1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Ellipse(Double.NaN, 0, 1, 1, 0));
    }

    @Test
    void derivedQuantities() {
        Ellipse e = new Ellipse(0, 0, 12, 9, 0);
        assertEquals(24, e.majorAxis(), 0);
        assertEquals(18, e.minorAxis(), 0);
        assertEquals(0.75, e.axisRatio(), 0);
        assertEquals(Math.PI * 12 * 9, e.area(), EPS);
        assertEquals(new Vec2(0, 0), e.center());
    }

    @Test
    void pointAtLiesOnTheImplicitCurve() {
        Ellipse e = new Ellipse(100, 50, 30, 10, 0.7);
        for (int k = 0; k < 100; k++) {
            double t = 0.0628 * k;
            Vec2 p = e.pointAt(t).minus(e.center());
            double c = Math.cos(e.theta);
            double s = Math.sin(e.theta);
            double lx = c * p.x + s * p.y;
            double ly = -s * p.x + c * p.y;
            assertEquals(1, (lx * lx) / 900 + (ly * ly) / 100, 1e-12);
        }
    }

    @Test
    void signedDistanceAxisAlignedKnownValues() {
        Ellipse e = new Ellipse(100, 50, 10, 5, 0);
        assertEquals(5, e.signedDistance(new Vec2(115, 50)), EPS);   // beyond the major vertex
        assertEquals(3, e.signedDistance(new Vec2(100, 58)), EPS);   // beyond the minor vertex (y down)
        assertEquals(-5, e.signedDistance(new Vec2(100, 50)), EPS);  // centre: nearest is the minor vertex
        assertEquals(-2, e.signedDistance(new Vec2(108, 50)), EPS);  // inside on the major axis
        assertEquals(0, e.signedDistance(new Vec2(110, 50)), EPS);
    }

    @Test
    void signedDistanceOfCircleCentreIsMinusRadius() {
        assertEquals(-7, new Ellipse(3, 4, 7, 7, 0).signedDistance(new Vec2(3, 4)), EPS);
    }

    @Test
    void signedDistanceMatchesBruteForceForRotatedEllipse() {
        Ellipse e = new Ellipse(200, 120, 40, 15, 1.1);
        Random rnd = new Random(7);
        for (int k = 0; k < 40; k++) {
            Vec2 p = new Vec2(200 + 120 * (rnd.nextDouble() - 0.5), 120 + 120 * (rnd.nextDouble() - 0.5));
            double best = Double.MAX_VALUE;
            int n = 400_000;
            for (int i = 0; i < n; i++) {
                best = Math.min(best, p.distanceTo(e.pointAt(2 * Math.PI * i / n)));
            }
            double d = e.signedDistance(p);
            assertEquals(best, Math.abs(d), 1e-6);
            assertEquals(e.contains(p), d < 0);
        }
    }

    @Test
    void footParameterRecoversPointAlongTheNormal() {
        Ellipse e = new Ellipse(-20, 35, 25, 18, 2.4);
        for (int k = 0; k < 50; k++) {
            double t = -Math.PI + 0.125 * k;
            Vec2 n = e.outwardNormalAt(t);
            for (double d : new double[] {-3, -0.5, 0, 0.5, 7}) {
                Vec2 p = e.pointAt(t).plus(n.times(d));
                assertEquals(0, e.pointAt(e.footParameter(p)).distanceTo(e.pointAt(t)), 1e-7);
                assertEquals(d, e.signedDistance(p), 1e-9);
            }
        }
    }

    @Test
    void outwardNormalIsUnitPerpendicularAndPointsOut() {
        Ellipse e = new Ellipse(0, 0, 9, 4, 0.5);
        for (int k = 0; k < 36; k++) {
            double t = k * Math.PI / 18;
            Vec2 n = e.outwardNormalAt(t);
            Vec2 tangent = e.pointAt(t + 1e-6).minus(e.pointAt(t - 1e-6));
            assertEquals(1, n.norm(), 1e-12);
            assertEquals(0, n.dot(tangent.normalized()), 1e-9);
            assertTrue(e.signedDistance(e.pointAt(t).plus(n.times(0.1))) > 0);
        }
    }
}
