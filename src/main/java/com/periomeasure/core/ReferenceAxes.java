package com.periomeasure.core;

import java.util.Objects;

/**
 * Reference axes for the measurements.
 * <ul>
 *   <li>{@code u}: unit "horizontal" axis. From pupils: OD pupil to OS
 *       pupil. From a user line or the image horizontal: oriented towards +x.</li>
 *   <li>{@code v}: unit axis perpendicular to u pointing UP in the image.
 *       Because y grows downwards, "up" means {@code v.y < 0}.</li>
 * </ul>
 * Vertical measurements use {@code d . v}; horizontal ones use {@code d . u}.
 */
public final class ReferenceAxes {

    public enum Source {
        /** u from the right (OD) to the left (OS) pupil centre. */
        PUPILS,
        /** u along a line drawn by the user (single-eye measurement). */
        USER_LINE,
        /** u along the image x axis (single-eye measurement). */
        IMAGE_HORIZONTAL
    }

    public final Vec2 u;
    public final Vec2 v;
    public final Source source;

    private ReferenceAxes(Vec2 u, Source source) {
        if (Math.abs(u.x) <= Math.abs(u.y)) {
            throw new IllegalArgumentException("Reference axis is more than 45 degrees from the image horizontal");
        }
        this.u = u;
        // The perpendicular whose y component is negative (up in the image).
        this.v = u.x > 0 ? new Vec2(u.y, -u.x) : new Vec2(-u.y, u.x);
        this.source = source;
    }

    /** @throws IllegalArgumentException if the pupils coincide or the axis is steeper than 45 degrees */
    public static ReferenceAxes fromPupils(Vec2 odPupil, Vec2 osPupil) {
        Objects.requireNonNull(odPupil, "odPupil");
        Objects.requireNonNull(osPupil, "osPupil");
        Vec2 d = osPupil.minus(odPupil);
        if (d.norm() == 0) {
            throw new IllegalArgumentException("Pupil centres coincide");
        }
        return new ReferenceAxes(d.normalized(), Source.PUPILS);
    }

    /** @throws IllegalArgumentException if the points coincide or the line is steeper than 45 degrees */
    public static ReferenceAxes fromLine(Vec2 p1, Vec2 p2) {
        Objects.requireNonNull(p1, "p1");
        Objects.requireNonNull(p2, "p2");
        Vec2 d = p2.minus(p1);
        if (d.norm() == 0) {
            throw new IllegalArgumentException("Line end points coincide");
        }
        if (d.x < 0) {
            d = d.times(-1);
        }
        return new ReferenceAxes(d.normalized(), Source.USER_LINE);
    }

    public static ReferenceAxes imageHorizontal() {
        return new ReferenceAxes(new Vec2(1, 0), Source.IMAGE_HORIZONTAL);
    }

    /**
     * In an unmirrored frontal photo the patient's right eye is on the image
     * left, so u points towards +x. If it points towards -x the image was
     * probably mirrored (or the eyes were labelled the wrong way round).
     */
    public boolean isMirrored() {
        return source == Source.PUPILS && u.x < 0;
    }

    /** @return angle of the horizontal reference line to the image x axis, degrees (y down) */
    public double tiltDegrees() {
        return Math.toDegrees(Math.atan(u.y / u.x));
    }

    /** @return unit vector pointing laterally (away from the nose) for {@code eye} */
    public Vec2 lateral(Eye eye) {
        return u.times(eye.lateralSign());
    }
}
