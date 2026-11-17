package com.periomeasure.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Least-squares ellipse fitting in two stages:
 * <ol>
 *   <li><b>Algebraic</b>: direct ellipse-specific fit of Fitzgibbon et al.
 *       in the numerically stable form of Halir &amp; Flusser (1998), on
 *       centred and scaled coordinates. Falls back to a Kasa circle fit if
 *       the algebraic step does not yield an ellipse.</li>
 *   <li><b>Geometric</b>: Levenberg-Marquardt minimisation of the sum of
 *       squared orthogonal distances, with the analytic Jacobian
 *       {@code dr/dp = -n . dX/dp} evaluated at the foot point.</li>
 * </ol>
 * Parameter standard errors come from {@code sigma^2 (J^T J)^-1} with
 * {@code sigma^2 = RSS / (n - 5)}.
 */
public final class EllipseFitter {

    public static final int MIN_POINTS = 5;

    private static final int NPARAM = 5;
    private static final int MAX_ITERATIONS = 1000;
    private static final double LAMBDA_MIN = 1e-6;
    private static final double LAMBDA_MAX = 1e16;

    private EllipseFitter() {
    }

    /**
     * @param points edge points in image coordinates (at least {@value #MIN_POINTS})
     * @throws IllegalArgumentException if fewer than {@value #MIN_POINTS} points
     * @throws EllipseFitException      if the points do not determine an ellipse
     */
    public static EllipseFitResult fit(List<Vec2> points) {
        int n = points.size();
        if (n < MIN_POINTS) {
            throw new IllegalArgumentException("At least " + MIN_POINTS + " points are required, got " + n);
        }
        if (new HashSet<>(points).size() < MIN_POINTS) {
            throw new EllipseFitException("At least " + MIN_POINTS + " distinct points are required");
        }

        // Centre and scale for conditioning: mean at origin, RMS distance sqrt(2).
        double mx = 0;
        double my = 0;
        for (Vec2 p : points) {
            mx += p.x;
            my += p.y;
        }
        mx /= n;
        my /= n;
        double ms = 0;
        for (Vec2 p : points) {
            ms += (p.x - mx) * (p.x - mx) + (p.y - my) * (p.y - my);
        }
        ms /= n;
        if (ms == 0) {
            throw new EllipseFitException("All points coincide");
        }
        double sc = Math.sqrt(ms / 2);
        List<Vec2> q = new ArrayList<>(n);
        for (Vec2 p : points) {
            q.add(new Vec2((p.x - mx) / sc, (p.y - my) / sc));
        }
        rejectCollinear(q);

        double[] init;
        try {
            init = halirFlusser(q);
        } catch (EllipseFitException | ArithmeticException e) {
            init = kasaCircle(q);
        }

        double[] r = new double[n];
        double[][] jac = new double[n][NPARAM];
        residualsAndJacobian(init, q, r, jac);
        double algebraicRms = Math.sqrt(sumSq(r) / n) * sc;

        double[] p = levenbergMarquardt(init, q);

        residualsAndJacobian(p, q, r, jac);
        double rss = sumSq(r);
        double[] sigmaAB = semiAxisSigmas(jac, rss, n);

        Ellipse e = new Ellipse(mx + sc * p[0], my + sc * p[1], sc * p[2], sc * p[3], p[4]);
        boolean swapped = p[3] > p[2];
        double sigmaMajor = 2 * sc * (swapped ? sigmaAB[1] : sigmaAB[0]);
        double sigmaMinor = 2 * sc * (swapped ? sigmaAB[0] : sigmaAB[1]);
        return new EllipseFitResult(e, Math.sqrt(rss / n) * sc, algebraicRms, sigmaMajor, sigmaMinor, n);
    }

    // ------------------------------------------------------------------ residuals

    /**
     * Signed orthogonal residuals (positive outside) and their Jacobian with
     * respect to p = (cx, cy, a, b, theta); a and b need not be ordered.
     */
    static void residualsAndJacobian(double[] p, List<Vec2> pts, double[] r, double[][] j) {
        double cx = p[0];
        double cy = p[1];
        double a = p[2];
        double b = p[3];
        double c = Math.cos(p[4]);
        double s = Math.sin(p[4]);
        for (int i = 0; i < pts.size(); i++) {
            Vec2 pt = pts.get(i);
            double dx = pt.x - cx;
            double dy = pt.y - cy;
            double lx = c * dx + s * dy;
            double ly = -s * dx + c * dy;
            double[] f = Ellipse.localFoot(a, b, lx, ly);
            double nlx = f[0] / (a * a);
            double nly = f[1] / (b * b);
            double nn = Math.hypot(nlx, nly);
            nlx /= nn;
            nly /= nn;
            r[i] = nlx * (lx - f[0]) + nly * (ly - f[1]);
            double cosT = f[0] / a;
            double sinT = f[1] / b;
            double nx = c * nlx - s * nly;
            double ny = s * nlx + c * nly;
            j[i][0] = -nx;
            j[i][1] = -ny;
            j[i][2] = -nlx * cosT;
            j[i][3] = -nly * sinT;
            j[i][4] = nlx * b * sinT - nly * a * cosT;
        }
    }

    // ------------------------------------------------------------------ geometric stage

    private static double[] levenbergMarquardt(double[] start, List<Vec2> q) {
        int n = q.size();
        double[] p = start.clone();
        double[] r = new double[n];
        double[][] jac = new double[n][NPARAM];
        residualsAndJacobian(p, q, r, jac);
        double cost = sumSq(r);
        double lambda = 1e-3;
        double[] rNew = new double[n];
        double[][] jNew = new double[n][NPARAM];

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            if (cost == 0) {
                return p;
            }
            double[][] jtj = new double[NPARAM][NPARAM];
            double[] jtr = new double[NPARAM];
            normalEquations(jac, r, jtj, jtr);
            double maxDiag = 0;
            for (int k = 0; k < NPARAM; k++) {
                maxDiag = Math.max(maxDiag, jtj[k][k]);
            }

            boolean accepted = false;
            while (!accepted) {
                double[][] aug = new double[NPARAM][NPARAM];
                double[] rhs = new double[NPARAM];
                for (int k = 0; k < NPARAM; k++) {
                    System.arraycopy(jtj[k], 0, aug[k], 0, NPARAM);
                    aug[k][k] += lambda * Math.max(jtj[k][k], 1e-6 * maxDiag);
                    rhs[k] = -jtr[k];
                }
                double[] step;
                try {
                    step = LinAlg.solve(aug, rhs);
                } catch (ArithmeticException e) {
                    step = null;
                }
                double[] trial = null;
                if (step != null) {
                    trial = new double[NPARAM];
                    for (int k = 0; k < NPARAM; k++) {
                        trial[k] = p[k] + step[k];
                    }
                    if (!(trial[2] > 0 && trial[3] > 0) || !allFinite(trial)) {
                        trial = null;
                    }
                }
                double newCost = Double.POSITIVE_INFINITY;
                if (trial != null) {
                    residualsAndJacobian(trial, q, rNew, jNew);
                    newCost = sumSq(rNew);
                }
                if (trial != null && newCost < cost) {
                    double decrease = cost - newCost;
                    double maxStep = 0;
                    for (int k = 0; k < NPARAM; k++) {
                        maxStep = Math.max(maxStep, Math.abs(step[k]));
                    }
                    p = trial;
                    System.arraycopy(rNew, 0, r, 0, n);
                    for (int i = 0; i < n; i++) {
                        System.arraycopy(jNew[i], 0, jac[i], 0, NPARAM);
                    }
                    cost = newCost;
                    lambda = Math.max(lambda / 10, LAMBDA_MIN);
                    accepted = true;
                    if (maxStep <= 1e-13 || decrease <= 1e-15 * (cost + decrease)) {
                        return p;
                    }
                } else {
                    lambda *= 10;
                    if (lambda > LAMBDA_MAX) {
                        // No step decreases the cost any more: numerical minimum reached.
                        return p;
                    }
                }
            }
        }
        throw new EllipseFitException("Geometric ellipse fit did not converge");
    }

    /**
     * Standard errors of the two semi-axes (normalised units). If the
     * orientation is unidentifiable (near-perfect circle), it is held fixed.
     */
    private static double[] semiAxisSigmas(double[][] jac, double rss, int n) {
        int dof = n - NPARAM;
        if (dof <= 0) {
            return new double[] {Double.NaN, Double.NaN};
        }
        double sigma2 = rss / dof;
        double[][] jtj = new double[NPARAM][NPARAM];
        normalEquations(jac, new double[n], jtj, new double[NPARAM]);
        double maxLen = Math.max(Math.max(jtj[0][0], jtj[1][1]), Math.max(jtj[2][2], jtj[3][3]));
        boolean thetaIdentifiable = jtj[4][4] > 1e-12 * maxLen;
        double[][] cov = null;
        if (thetaIdentifiable) {
            cov = scaledInverse(jtj, NPARAM);
        }
        if (cov == null) {
            cov = scaledInverse(jtj, 4);
        }
        if (cov == null) {
            throw new EllipseFitException("Ellipse parameters are not determined by the points");
        }
        return new double[] {Math.sqrt(sigma2 * cov[2][2]), Math.sqrt(sigma2 * cov[3][3])};
    }

    /** Inverse of the leading m x m block using Jacobi scaling; null if singular. */
    private static double[][] scaledInverse(double[][] jtj, int m) {
        double[] d = new double[m];
        for (int k = 0; k < m; k++) {
            if (!(jtj[k][k] > 0)) {
                return null;
            }
            d[k] = 1 / Math.sqrt(jtj[k][k]);
        }
        double[][] s = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < m; k++) {
                s[i][k] = jtj[i][k] * d[i] * d[k];
            }
        }
        double[][] inv;
        try {
            inv = LinAlg.inverse(s);
        } catch (ArithmeticException e) {
            return null;
        }
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < m; k++) {
                inv[i][k] *= d[i] * d[k];
            }
        }
        return inv;
    }

    // ------------------------------------------------------------------ algebraic stage

    /** Halir-Flusser direct ellipse fit; returns raw parameters (cx, cy, a, b, theta). */
    static double[] halirFlusser(List<Vec2> q) {
        double[][] s1 = new double[3][3];
        double[][] s2 = new double[3][3];
        double[][] s3 = new double[3][3];
        for (Vec2 p : q) {
            double[] d1 = {p.x * p.x, p.x * p.y, p.y * p.y};
            double[] d2 = {p.x, p.y, 1};
            for (int i = 0; i < 3; i++) {
                for (int k = 0; k < 3; k++) {
                    s1[i][k] += d1[i] * d1[k];
                    s2[i][k] += d1[i] * d2[k];
                    s3[i][k] += d2[i] * d2[k];
                }
            }
        }
        double[][] s3inv = LinAlg.inverse(s3);
        double[][] t = new double[3][3]; // T = -S3^-1 S2^T
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                double v = 0;
                for (int m = 0; m < 3; m++) {
                    v -= s3inv[i][m] * s2[k][m];
                }
                t[i][k] = v;
            }
        }
        double[][] mm = new double[3][3]; // S1 + S2 T
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                double v = s1[i][k];
                for (int m = 0; m < 3; m++) {
                    v += s2[i][m] * t[m][k];
                }
                mm[i][k] = v;
            }
        }
        double[][] m = new double[3][3]; // C1^-1 (S1 + S2 T)
        for (int k = 0; k < 3; k++) {
            m[0][k] = mm[2][k] / 2;
            m[1][k] = -mm[1][k];
            m[2][k] = mm[0][k] / 2;
        }

        double[] best = null;
        double bestLambda = Double.POSITIVE_INFINITY;
        for (double lambda : Eigen3.realEigenvalues(m)) {
            double[] v = Eigen3.eigenvector(m, lambda);
            if (v == null) {
                continue;
            }
            double cond = 4 * v[0] * v[2] - v[1] * v[1];
            if (cond > 0 && lambda < bestLambda) {
                best = v;
                bestLambda = lambda;
            }
        }
        if (best == null) {
            throw new EllipseFitException("Algebraic fit did not produce an ellipse");
        }
        double[] a2 = new double[3];
        for (int i = 0; i < 3; i++) {
            a2[i] = t[i][0] * best[0] + t[i][1] * best[1] + t[i][2] * best[2];
        }
        return conicToParameters(best[0], best[1], best[2], a2[0], a2[1], a2[2]);
    }

    /** A x^2 + B xy + C y^2 + D x + E y + F = 0  ->  (cx, cy, a, b, theta). */
    static double[] conicToParameters(double a, double b, double c, double d, double e, double f) {
        if (a + c < 0) {
            a = -a;
            b = -b;
            c = -c;
            d = -d;
            e = -e;
            f = -f;
        }
        double den = 4 * a * c - b * b;
        if (!(den > 0)) {
            throw new EllipseFitException("Conic is not an ellipse");
        }
        double x0 = (b * e - 2 * c * d) / den;
        double y0 = (b * d - 2 * a * e) / den;
        double f0 = f + (d * x0 + e * y0) / 2;
        double h = Math.hypot((a - c) / 2, b / 2);
        double lMax = (a + c) / 2 + h;
        double lMin = (a + c) / 2 - h;
        if (!(lMin > 0) || !(f0 < 0)) {
            throw new EllipseFitException("Conic is not a real ellipse");
        }
        double th = 0.5 * Math.atan2(b, a - c);
        double[] out = {x0, y0, Math.sqrt(-f0 / lMax), Math.sqrt(-f0 / lMin), th};
        if (!allFinite(out)) {
            throw new EllipseFitException("Degenerate conic");
        }
        return out;
    }

    /** Kasa algebraic circle fit, used only as a fallback starting point. */
    static double[] kasaCircle(List<Vec2> q) {
        double[][] nmat = new double[3][3];
        double[] rhs = new double[3];
        for (Vec2 p : q) {
            double[] row = {p.x, p.y, 1};
            double z = -(p.x * p.x + p.y * p.y);
            for (int i = 0; i < 3; i++) {
                for (int k = 0; k < 3; k++) {
                    nmat[i][k] += row[i] * row[k];
                }
                rhs[i] += row[i] * z;
            }
        }
        double[] sol;
        try {
            sol = LinAlg.solve(nmat, rhs);
        } catch (ArithmeticException e) {
            throw new EllipseFitException("Points do not determine a circle or ellipse");
        }
        double x0 = -sol[0] / 2;
        double y0 = -sol[1] / 2;
        double r2 = x0 * x0 + y0 * y0 - sol[2];
        if (!(r2 > 0)) {
            throw new EllipseFitException("Points do not determine a circle or ellipse");
        }
        double r = Math.sqrt(r2);
        return new double[] {x0, y0, r, r, 0};
    }

    // ------------------------------------------------------------------ helpers

    private static void rejectCollinear(List<Vec2> q) {
        double sxx = 0;
        double sxy = 0;
        double syy = 0;
        for (Vec2 p : q) {
            sxx += p.x * p.x;
            sxy += p.x * p.y;
            syy += p.y * p.y;
        }
        double mean = (sxx + syy) / 2;
        double h = Math.hypot((sxx - syy) / 2, sxy);
        if (mean - h <= 1e-10 * (mean + h)) {
            throw new EllipseFitException("Points are collinear");
        }
    }

    private static void normalEquations(double[][] jac, double[] r, double[][] jtj, double[] jtr) {
        for (int i = 0; i < jac.length; i++) {
            double[] row = jac[i];
            for (int a = 0; a < NPARAM; a++) {
                jtr[a] += row[a] * r[i];
                for (int b = 0; b < NPARAM; b++) {
                    jtj[a][b] += row[a] * row[b];
                }
            }
        }
    }

    private static double sumSq(double[] r) {
        double s = 0;
        for (double v : r) {
            s += v * v;
        }
        return s;
    }

    private static boolean allFinite(double[] v) {
        for (double x : v) {
            if (Double.isNaN(x) || Double.isInfinite(x)) {
                return false;
            }
        }
        return true;
    }
}
