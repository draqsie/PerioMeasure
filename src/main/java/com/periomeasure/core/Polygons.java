package com.periomeasure.core;

import java.util.List;

/** Polygon helpers. The polygon is closed implicitly (last vertex connects to the first). */
public final class Polygons {

    private Polygons() {
    }

    /**
     * Shoelace formula, computed relative to the first vertex for accuracy far
     * from the origin.
     *
     * @return unsigned area (independent of vertex order and orientation)
     */
    public static double area(List<Vec2> polygon) {
        check(polygon);
        Vec2 o = polygon.get(0);
        double twice = 0;
        for (int i = 1; i < polygon.size() - 1; i++) {
            twice += polygon.get(i).minus(o).cross(polygon.get(i + 1).minus(o));
        }
        return Math.abs(twice) / 2;
    }

    public static double perimeter(List<Vec2> polygon) {
        check(polygon);
        double sum = 0;
        for (int i = 0; i < polygon.size(); i++) {
            sum += polygon.get(i).distanceTo(polygon.get((i + 1) % polygon.size()));
        }
        return sum;
    }

    private static void check(List<Vec2> polygon) {
        if (polygon.size() < 3) {
            throw new IllegalArgumentException("A polygon needs at least 3 vertices, got " + polygon.size());
        }
    }
}
