package com.periomeasure.core;

import java.util.ArrayList;
import java.util.List;

/** Real eigenvalues / eigenvectors of a general (non-symmetric) 3x3 matrix. */
final class Eigen3 {

    private Eigen3() {
    }

    /** @return the real eigenvalues (1 or 3 values; repeated roots may appear twice) */
    static List<Double> realEigenvalues(double[][] m) {
        double tr = m[0][0] + m[1][1] + m[2][2];
        double minors = (m[0][0] * m[1][1] - m[0][1] * m[1][0])
                + (m[0][0] * m[2][2] - m[0][2] * m[2][0])
                + (m[1][1] * m[2][2] - m[1][2] * m[2][1]);
        double det = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
        // lambda^3 + c2 lambda^2 + c1 lambda + c0 = 0
        double c2 = -tr;
        double c1 = minors;
        double c0 = -det;
        List<Double> roots = new ArrayList<>();
        double shift = c2 / 3;
        double p = c1 - c2 * c2 / 3;
        double q = 2 * c2 * c2 * c2 / 27 - c2 * c1 / 3 + c0;
        double disc = (q / 2) * (q / 2) + (p / 3) * (p / 3) * (p / 3);
        if (p == 0 && q == 0) {
            roots.add(-shift);
        } else if (disc > 0) {
            double sq = Math.sqrt(disc);
            roots.add(Math.cbrt(-q / 2 + sq) + Math.cbrt(-q / 2 - sq) - shift);
        } else {
            double r = Math.sqrt(-p / 3);
            double cosArg = -q / (2 * r * r * r);
            cosArg = Math.max(-1, Math.min(1, cosArg));
            double phi = Math.acos(cosArg);
            for (int k = 0; k < 3; k++) {
                roots.add(2 * r * Math.cos((phi + 2 * Math.PI * k) / 3) - shift);
            }
        }
        List<Double> polished = new ArrayList<>();
        for (double x : roots) {
            for (int it = 0; it < 4; it++) {
                double f = ((x + c2) * x + c1) * x + c0;
                double df = (3 * x + 2 * c2) * x + c1;
                if (df == 0) {
                    break;
                }
                double nx = x - f / df;
                if (Double.isNaN(nx) || Double.isInfinite(nx)) {
                    break;
                }
                x = nx;
            }
            polished.add(x);
        }
        return polished;
    }

    /** @return unit eigenvector for {@code lambda}, or null if it cannot be determined */
    static double[] eigenvector(double[][] m, double lambda) {
        double[][] b = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                b[i][k] = m[i][k] - (i == k ? lambda : 0);
            }
        }
        double[][] candidates = {cross(b[0], b[1]), cross(b[0], b[2]), cross(b[1], b[2])};
        double[] v = null;
        double best = 0;
        for (double[] c : candidates) {
            double n = norm(c);
            if (n > best) {
                best = n;
                v = c;
            }
        }
        if (v == null || !(best > 0)) {
            return null;
        }
        scale(v, 1 / best);
        // Inverse iteration to polish the eigenvector.
        double mnorm = 0;
        for (double[] row : m) {
            for (double x : row) {
                mnorm += x * x;
            }
        }
        double eps = 1e-8 * (Math.sqrt(mnorm) + Math.abs(lambda));
        for (int it = 0; it < 3; it++) {
            double[][] sh = new double[3][3];
            for (int i = 0; i < 3; i++) {
                for (int k = 0; k < 3; k++) {
                    sh[i][k] = m[i][k] - (i == k ? lambda + eps : 0);
                }
            }
            double[] w;
            try {
                w = LinAlg.solve(sh, v);
            } catch (ArithmeticException e) {
                break;
            }
            double n = norm(w);
            if (!(n > 0) || Double.isInfinite(n)) {
                break;
            }
            scale(w, 1 / n);
            v = w;
        }
        return v;
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double norm(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static void scale(double[] v, double k) {
        for (int i = 0; i < v.length; i++) {
            v[i] *= k;
        }
    }
}
