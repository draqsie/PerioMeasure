package com.periomeasure.core;

import java.util.Locale;

/**
 * Immutable 2-D point / vector in continuous image coordinates
 * (x to the right, y DOWN; see {@link PixelConvention}). Coordinates are
 * always finite.
 */
public final class Vec2 {

    public final double x;
    public final double y;

    public Vec2(double x, double y) {
        if (!isFinite(x) || !isFinite(y)) {
            throw new IllegalArgumentException("Non-finite coordinate: (" + x + ", " + y + ")");
        }
        this.x = x;
        this.y = y;
    }

    public Vec2 plus(Vec2 o) {
        return new Vec2(x + o.x, y + o.y);
    }

    public Vec2 minus(Vec2 o) {
        return new Vec2(x - o.x, y - o.y);
    }

    public Vec2 times(double k) {
        return new Vec2(x * k, y * k);
    }

    public double dot(Vec2 o) {
        return x * o.x + y * o.y;
    }

    /** z component of the 3-D cross product (this, 0) x (o, 0). */
    public double cross(Vec2 o) {
        return x * o.y - y * o.x;
    }

    public double norm() {
        return Math.hypot(x, y);
    }

    public double distanceTo(Vec2 o) {
        return Math.hypot(x - o.x, y - o.y);
    }

    /** @throws IllegalArgumentException for the zero vector */
    public Vec2 normalized() {
        double n = norm();
        if (n == 0) {
            throw new IllegalArgumentException("Cannot normalise the zero vector");
        }
        return new Vec2(x / n, y / n);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vec2)) {
            return false;
        }
        Vec2 v = (Vec2) o;
        return x == v.x && y == v.y; // coordinates are finite; -0.0 equals 0.0
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(x == 0 ? 0.0 : x) + Double.hashCode(y == 0 ? 0.0 : y);
    }

    @Override
    public String toString() {
        return "(" + fmt(x) + ", " + fmt(y) + ")";
    }

    private static String fmt(double v) {
        String s = String.format(Locale.ROOT, "%.10f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}
