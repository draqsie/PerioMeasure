package com.periomeasure.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class EllipseFitterTest {

    /** Points on {@code e} at parameters evenly spread over [t0, t0 + span). */
    static List<Vec2> sample(Ellipse e, int n, double t0, double span, double noiseSigma, Random rnd) {
        List<Vec2> pts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double t = t0 + span * i / n;
            Vec2 p = e.pointAt(t);
            if (noiseSigma > 0) {
                p = p.plus(new Vec2(rnd.nextGaussian() * noiseSigma, rnd.nextGaussian() * noiseSigma));
            }
            pts.add(p);
        }
        return pts;
    }

    private static double angleDiffModPi(double a, double b) {
        double d = Math.abs(a - b) % Math.PI;
        return Math.min(d, Math.PI - d);
    }

    private static void assertSameEllipse(Ellipse expected, Ellipse actual, double tol) {
        assertEquals(expected.cx, actual.cx, tol, "cx");
        assertEquals(expected.cy, actual.cy, tol, "cy");
        assertEquals(expected.a, actual.a, tol, "a");
        assertEquals(expected.b, actual.b, tol, "b");
        if (expected.axisRatio() < 0.999) {
            assertEquals(0, angleDiffModPi(expected.theta, actual.theta), tol / expected.a * 10, "theta");
        }
    }

    @Test
    void exactPointsRecoverTheEllipseForManyShapes() {
        double[] semiMajors = {75, 120, 300, 600};
        double[] ratios = {1.0, 0.99, 0.97, 0.9, 0.7};
        double[] thetas = {0, 0.3, 1.2, 2.9};
        int[] counts = {8, 12, 40};
        for (double a : semiMajors) {
            for (double r : ratios) {
                for (double th : thetas) {
                    for (int n : counts) {
                        Ellipse truth = new Ellipse(1500.25, 812.75, a, a * r, th);
                        EllipseFitResult fit = EllipseFitter.fit(sample(truth, n, 0.1, 2 * Math.PI, 0, null));
                        assertSameEllipse(truth, fit.ellipse, 1e-6);
                        assertTrue(fit.rmsResidualPx < 1e-6, "rms " + fit.rmsResidualPx);
                    }
                }
            }
        }
    }

    /** The algebraic stage on its own (no circle fallback, no refinement) must be exact on exact data. */
    @Test
    void algebraicStageAloneRecoversExactEllipse() {
        for (double ratio : new double[] {0.999, 0.97, 0.6}) {
            Ellipse truth = new Ellipse(0.3, -0.2, 1.4, 1.4 * ratio, 0.7);
            double[] p = EllipseFitter.halirFlusser(sample(truth, 12, 0, 2 * Math.PI, 0, null));
            assertSameEllipse(truth, new Ellipse(p[0], p[1], p[2], p[3], p[4]), 1e-9);
        }
    }

    @Test
    void conicConversionOfKnownCircle() {
        // (x-1)^2 + (y+2)^2 = 9  ->  x^2 + y^2 - 2x + 4y - 4 = 0
        double[] p = EllipseFitter.conicToParameters(1, 0, 1, -2, 4, -4);
        assertEquals(1, p[0], 1e-12);
        assertEquals(-2, p[1], 1e-12);
        assertEquals(3, p[2], 1e-12);
        assertEquals(3, p[3], 1e-12);
        assertThrows(EllipseFitException.class, () -> EllipseFitter.conicToParameters(1, 0, -1, 0, 0, -1));
    }

    @Test
    void partialArcIsRecovered() {
        Ellipse truth = new Ellipse(400, 300, 150, 140, 0.4);
        EllipseFitResult fit = EllipseFitter.fit(sample(truth, 16, 0.5, Math.toRadians(200), 0, null));
        assertSameEllipse(truth, fit.ellipse, 1e-6);
    }

    @Test
    void farFromOriginCoordinatesAreHandled() {
        Ellipse truth = new Ellipse(5800.5, 3900.5, 160, 155, 1.0);
        EllipseFitResult fit = EllipseFitter.fit(sample(truth, 10, 0, 2 * Math.PI, 0, null));
        assertSameEllipse(truth, fit.ellipse, 1e-6);
    }

    @Test
    void resultDoesNotDependOnPointOrder() {
        Ellipse truth = new Ellipse(300, 200, 110, 100, 0.8);
        List<Vec2> pts = sample(truth, 20, 0, 2 * Math.PI, 0.6, new Random(3));
        EllipseFitResult a = EllipseFitter.fit(pts);
        List<Vec2> shuffled = new ArrayList<>(pts);
        Collections.shuffle(shuffled, new Random(11));
        EllipseFitResult b = EllipseFitter.fit(shuffled);
        assertSameEllipse(a.ellipse, b.ellipse, 1e-8);
    }

    @Test
    void geometricRefinementNeverIncreasesTheGeometricError() {
        Random rnd = new Random(5);
        for (int k = 0; k < 50; k++) {
            Ellipse truth = new Ellipse(500, 500, 100 + 50 * rnd.nextDouble(), 90, rnd.nextDouble() * Math.PI);
            EllipseFitResult fit = EllipseFitter.fit(sample(truth, 12, rnd.nextDouble(), 2 * Math.PI, 1.0, rnd));
            assertTrue(fit.rmsResidualPx <= fit.algebraicRmsResidualPx + 1e-12,
                    fit.rmsResidualPx + " > " + fit.algebraicRmsResidualPx);
        }
    }

    /**
     * Monte Carlo check of the reported uncertainty: with noisy edge clicks the
     * reported sigma of the major axis must match the empirical spread, and the
     * true value must fall inside +/- 2 sigma about 95 % of the time.
     */
    @Test
    void reportedMajorAxisUncertaintyMatchesMonteCarloSpread() {
        Ellipse truth = new Ellipse(800, 600, 150, 145, 0.6);
        double noise = 0.5;
        int trials = 400;
        Random rnd = new Random(2026);
        double sum = 0;
        double sumSq = 0;
        double sumSigma = 0;
        int inside2Sigma = 0;
        for (int k = 0; k < trials; k++) {
            EllipseFitResult fit = EllipseFitter.fit(sample(truth, 24, rnd.nextDouble(), 2 * Math.PI, noise, rnd));
            double err = fit.ellipse.majorAxis() - truth.majorAxis();
            sum += err;
            sumSq += err * err;
            sumSigma += fit.sigmaMajorAxisPx;
            if (Math.abs(err) <= 2 * fit.sigmaMajorAxisPx) {
                inside2Sigma++;
            }
        }
        double mean = sum / trials;
        double sd = Math.sqrt(sumSq / trials - mean * mean);
        double meanSigma = sumSigma / trials;
        assertEquals(0, mean, 0.03, "bias of major axis (px)");
        assertEquals(1, meanSigma / sd, 0.2, "reported sigma / empirical SD");
        double coverage = inside2Sigma / (double) trials;
        assertTrue(coverage > 0.90 && coverage < 0.99, "2-sigma coverage " + coverage);
    }

    @Test
    void rmsResidualReflectsClickNoise() {
        Ellipse truth = new Ellipse(800, 600, 200, 190, 0.6);
        EllipseFitResult fit = EllipseFitter.fit(sample(truth, 200, 0, 2 * Math.PI, 0.8, new Random(9)));
        // Orthogonal residual of isotropic 2-D noise has SD = sigma.
        assertEquals(0.8, fit.rmsResidualPx, 0.12);
        assertEquals(200, fit.pointCount);
    }

    @Test
    void perfectCircleHasFiniteAxisUncertainty() {
        Ellipse truth = new Ellipse(300, 300, 120, 120, 0);
        EllipseFitResult fit = EllipseFitter.fit(sample(truth, 16, 0, 2 * Math.PI, 0, null));
        assertTrue(fit.sigmaMajorAxisPx >= 0 && fit.sigmaMajorAxisPx < 1e-6, "sigma " + fit.sigmaMajorAxisPx);
        assertEquals(1, fit.ellipse.axisRatio(), 1e-9);
    }

    @Test
    void jacobianMatchesFiniteDifferences() {
        double[] p = {210, 145, 60, 35, 0.9};
        List<Vec2> pts = sample(new Ellipse(205, 150, 64, 33, 0.85), 15, 0.2, 2 * Math.PI, 2.0, new Random(1));
        int n = pts.size();
        double[] r = new double[n];
        double[][] j = new double[n][5];
        EllipseFitter.residualsAndJacobian(p, pts, r, j);
        for (int k = 0; k < 5; k++) {
            double h = 1e-6 * Math.max(1, Math.abs(p[k]));
            double[] pp = p.clone();
            double[] pm = p.clone();
            pp[k] += h;
            pm[k] -= h;
            double[] rp = new double[n];
            double[] rm = new double[n];
            EllipseFitter.residualsAndJacobian(pp, pts, rp, new double[n][5]);
            EllipseFitter.residualsAndJacobian(pm, pts, rm, new double[n][5]);
            for (int i = 0; i < n; i++) {
                assertEquals((rp[i] - rm[i]) / (2 * h), j[i][k], 1e-5, "d r" + i + " / d p" + k);
            }
        }
    }

    @Test
    void tooFewPointsAreRejected() {
        List<Vec2> pts = sample(new Ellipse(0, 0, 10, 8, 0), 4, 0, 2 * Math.PI, 0, null);
        assertThrows(IllegalArgumentException.class, () -> EllipseFitter.fit(pts));
    }

    @Test
    void collinearPointsAreRejected() {
        List<Vec2> pts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pts.add(new Vec2(i, 2 * i + 1));
        }
        assertThrows(EllipseFitException.class, () -> EllipseFitter.fit(pts));
    }

    @Test
    void identicalPointsAreRejected() {
        List<Vec2> pts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pts.add(new Vec2(5, 5));
        }
        assertThrows(EllipseFitException.class, () -> EllipseFitter.fit(pts));
    }
}
