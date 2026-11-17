package com.periomeasure.core;

/**
 * Immutable ellipse in continuous image coordinates (x right, y down).
 *
 * <p>Parametrisation: {@code P(t) = C + R(theta) (a cos t, b sin t)} with
 * {@code R(theta) = [[cos, -sin], [sin, cos]]}. The constructor normalises so
 * that {@code a >= b > 0} (a = semi-major axis) and {@code theta} in [0, pi).
 */
public final class Ellipse {

    /** Centre x. */
    public final double cx;
    /** Centre y. */
    public final double cy;
    /** Semi-major axis (px), always >= b. */
    public final double a;
    /** Semi-minor axis (px). */
    public final double b;
    /** Direction of the major axis (radians), in [0, pi). */
    public final double theta;

    /**
     * @param semiAxis1 semi-axis along direction {@code theta}
     * @param semiAxis2 semi-axis along direction {@code theta + pi/2}
     */
    public Ellipse(double cx, double cy, double semiAxis1, double semiAxis2, double theta) {
        if (!finite(cx) || !finite(cy) || !finite(semiAxis1) || !finite(semiAxis2) || !finite(theta)) {
            throw new IllegalArgumentException("Non-finite ellipse parameter");
        }
        if (semiAxis1 <= 0 || semiAxis2 <= 0) {
            throw new IllegalArgumentException("Semi-axes must be positive: " + semiAxis1 + ", " + semiAxis2);
        }
        double th = theta;
        double big = semiAxis1;
        double small = semiAxis2;
        if (semiAxis2 > semiAxis1) {
            big = semiAxis2;
            small = semiAxis1;
            th += Math.PI / 2;
        }
        th = th - Math.PI * Math.floor(th / Math.PI);
        if (th >= Math.PI) {
            th -= Math.PI;
        }
        this.cx = cx;
        this.cy = cy;
        this.a = big;
        this.b = small;
        this.theta = th;
    }

    public Vec2 center() {
        return new Vec2(cx, cy);
    }

    /** @return full major axis length 2a (px) */
    public double majorAxis() {
        return 2 * a;
    }

    /** @return full minor axis length 2b (px) */
    public double minorAxis() {
        return 2 * b;
    }

    /** @return b / a, in (0, 1] */
    public double axisRatio() {
        return b / a;
    }

    public double area() {
        return Math.PI * a * b;
    }

    public Vec2 pointAt(double t) {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        double x = a * Math.cos(t);
        double y = b * Math.sin(t);
        return new Vec2(cx + c * x - s * y, cy + s * x + c * y);
    }

    /** @return outward unit normal at parameter {@code t} */
    public Vec2 outwardNormalAt(double t) {
        double nx = Math.cos(t) / a;
        double ny = Math.sin(t) / b;
        double n = Math.hypot(nx, ny);
        nx /= n;
        ny /= n;
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        return new Vec2(c * nx - s * ny, s * nx + c * ny);
    }

    /** @return parameter t of the point of this ellipse closest to {@code p} */
    public double footParameter(Vec2 p) {
        double[] l = toLocal(p);
        double[] f = localFoot(a, b, l[0], l[1]);
        return Math.atan2(f[1] / b, f[0] / a);
    }

    /** @return orthogonal distance to the curve; negative inside, positive outside (px) */
    public double signedDistance(Vec2 p) {
        double[] l = toLocal(p);
        double[] f = localFoot(a, b, l[0], l[1]);
        double d = Math.hypot(l[0] - f[0], l[1] - f[1]);
        return insideLocal(l[0], l[1]) ? -d : d;
    }

    /** @return true if {@code p} is strictly inside the ellipse */
    public boolean contains(Vec2 p) {
        double[] l = toLocal(p);
        return insideLocal(l[0], l[1]);
    }

    private boolean insideLocal(double lx, double ly) {
        double u = lx / a;
        double v = ly / b;
        return u * u + v * v < 1;
    }

    private double[] toLocal(Vec2 p) {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        double dx = p.x - cx;
        double dy = p.y - cy;
        return new double[] {c * dx + s * dy, -s * dx + c * dy};
    }

    /**
     * Closest point on the axis-aligned ellipse x^2/a^2 + y^2/b^2 = 1 to the
     * local point (lx, ly). Works for any a, b > 0 (a may be smaller than b).
     */
    static double[] localFoot(double a, double b, double lx, double ly) {
        boolean swap = b > a;
        double e0 = swap ? b : a;
        double e1 = swap ? a : b;
        double y0 = swap ? ly : lx;
        double y1 = swap ? lx : ly;
        double[] f = footFirstQuadrant(e0, e1, Math.abs(y0), Math.abs(y1));
        double f0 = Math.copySign(f[0], y0);
        double f1 = Math.copySign(f[1], y1);
        return swap ? new double[] {f1, f0} : new double[] {f0, f1};
    }

    /**
     * D. Eberly, "Distance from a Point to an Ellipse, an Ellipsoid, or a
     * Hyperellipsoid" (Geometric Tools, robust bisection version).
     * Requires e0 >= e1 > 0 and y0, y1 >= 0.
     */
    private static double[] footFirstQuadrant(double e0, double e1, double y0, double y1) {
        if (y1 > 0) {
            if (y0 > 0) {
                double z0 = y0 / e0;
                double z1 = y1 / e1;
                double g = z0 * z0 + z1 * z1 - 1;
                if (g != 0) {
                    double r0 = (e0 / e1) * (e0 / e1);
                    double sbar = rootOf(r0, z0, z1, g);
                    return new double[] {r0 * y0 / (sbar + r0), y1 / (sbar + 1)};
                }
                return new double[] {y0, y1};
            }
            return new double[] {0, e1};
        }
        double numer0 = e0 * y0;
        double denom0 = e0 * e0 - e1 * e1;
        if (numer0 < denom0) {
            double xde0 = numer0 / denom0;
            return new double[] {e0 * xde0, e1 * Math.sqrt(1 - xde0 * xde0)};
        }
        return new double[] {e0, 0};
    }

    private static double rootOf(double r0, double z0, double z1, double g) {
        double n0 = r0 * z0;
        double s0 = z1 - 1;
        double s1 = g < 0 ? 0 : Math.hypot(n0, z1) - 1;
        double s = 0;
        for (int i = 0; i < 2200; i++) { // bisection to full double precision
            s = 0.5 * (s0 + s1);
            if (s == s0 || s == s1) {
                break;
            }
            double ratio0 = n0 / (s + r0);
            double ratio1 = z1 / (s + 1);
            double gs = ratio0 * ratio0 + ratio1 * ratio1 - 1;
            if (gs > 0) {
                s0 = s;
            } else if (gs < 0) {
                s1 = s;
            } else {
                break;
            }
        }
        return s;
    }

    private static boolean finite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "Ellipse[c=(%.6f, %.6f), a=%.6f, b=%.6f, theta=%.6f]",
                cx, cy, a, b, theta);
    }
}
