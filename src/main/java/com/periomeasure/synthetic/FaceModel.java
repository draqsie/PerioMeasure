package com.periomeasure.synthetic;

import java.util.ArrayList;
import java.util.List;

import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.Vec2;

/**
 * Synthetic face geometry in MILLIMETRES, in a face frame:
 * X towards the patient's LEFT, Y UP, origin between the pupils.
 * (This frame differs from image coordinates on purpose: the scene transform
 * flips, rotates, scales and optionally mirrors it.)
 */
public final class FaceModel {

    /** Iris (limbus) radius used for limbus landmarks and rendering, mm. */
    public static final double LIMBUS_RADIUS_MM = 5.75;

    public final String name;
    /** Right eye landmarks (mm, face frame). */
    public final EyeLandmarks od;
    /** Left eye landmarks (mm, face frame). */
    public final EyeLandmarks os;
    public final Vec2 stickerCenterMm;
    public final double stickerDiameterMm;

    private FaceModel(String name, EyeLandmarks od, EyeLandmarks os, Vec2 stickerCenterMm, double stickerDiameterMm) {
        this.name = name;
        this.od = od;
        this.os = os;
        this.stickerCenterMm = stickerCenterMm;
        this.stickerDiameterMm = stickerDiameterMm;
    }

    /** Normal adult: MRD1 4.0 / 5.5 mm, positive canthal tilt, IPD 62.5 mm. */
    public static FaceModel typical() {
        return new FaceModel("typical",
                eye(new Vec2(-31.0, 0.0), 4.0, 5.5, new Vec2(-16.5, -0.8), new Vec2(-45.0, 1.2)),
                eye(new Vec2(31.5, 0.0), 3.6, 5.2, new Vec2(16.8, -0.6), new Vec2(45.6, 1.5)),
                new Vec2(0, 45), 24.0);
    }

    /** Right upper lid 1 mm BELOW the light reflex (MRD1 = -1.0 mm), negative right canthal tilt. */
    public static FaceModel rightPtosis() {
        return new FaceModel("right_ptosis",
                eye(new Vec2(-31.0, 0.0), -1.0, 5.5, new Vec2(-16.5, -0.8), new Vec2(-45.0, -2.0)),
                eye(new Vec2(31.5, 0.0), 3.6, 5.2, new Vec2(16.8, -0.6), new Vec2(45.6, 1.5)),
                new Vec2(0, 45), 24.0);
    }

    /** Pupils at different heights (1.3 mm) and asymmetric fissures. */
    public static FaceModel asymmetric() {
        return new FaceModel("asymmetric",
                eye(new Vec2(-30.8, 0.0), 2.4, 6.1, new Vec2(-16.0, -1.1), new Vec2(-44.1, 0.4)),
                eye(new Vec2(31.9, 1.3), 4.7, 4.9, new Vec2(17.2, 0.9), new Vec2(46.4, 4.0)),
                new Vec2(0.7, 46), 24.0);
    }

    private static EyeLandmarks eye(Vec2 pupil, double mrd1, double mrd2, Vec2 mc, Vec2 lc) {
        Vec2 upper = new Vec2(pupil.x, pupil.y + mrd1);
        Vec2 lower = new Vec2(pupil.x, pupil.y - mrd2);
        return EyeLandmarks.builder()
                .pupil(pupil)
                .upperLid(upper)
                .lowerLid(lower)
                .medialCanthus(mc)
                .lateralCanthus(lc)
                .upperLidCrease(new Vec2(pupil.x, upper.y + 5.0))
                .browLowerBorder(new Vec2(pupil.x, pupil.y + 22.0))
                .limbusUpper(new Vec2(pupil.x, pupil.y + LIMBUS_RADIUS_MM))
                .limbusLower(new Vec2(pupil.x, pupil.y - LIMBUS_RADIUS_MM))
                .contour(fissureContour(mc, upper, lower, lc, 24))
                .build();
    }

    /**
     * Fissure outline: the upper and lower lid margins are parabolas (in X)
     * through medial canthus, lid point and lateral canthus.
     */
    static List<Vec2> fissureContour(Vec2 mc, Vec2 upper, Vec2 lower, Vec2 lc, int perLid) {
        List<Vec2> poly = new ArrayList<>();
        for (int i = 0; i <= perLid; i++) {
            double x = mc.x + (lc.x - mc.x) * i / perLid;
            poly.add(new Vec2(x, parabola(mc, upper, lc, x)));
        }
        for (int i = perLid - 1; i >= 1; i--) {
            double x = mc.x + (lc.x - mc.x) * i / perLid;
            poly.add(new Vec2(x, parabola(mc, lower, lc, x)));
        }
        return poly;
    }

    /** Lagrange parabola y(x) through three points with distinct x. */
    static double parabola(Vec2 p0, Vec2 p1, Vec2 p2, double x) {
        double l0 = (x - p1.x) * (x - p2.x) / ((p0.x - p1.x) * (p0.x - p2.x));
        double l1 = (x - p0.x) * (x - p2.x) / ((p1.x - p0.x) * (p1.x - p2.x));
        double l2 = (x - p0.x) * (x - p1.x) / ((p2.x - p0.x) * (p2.x - p1.x));
        return p0.y * l0 + p1.y * l1 + p2.y * l2;
    }

    public EyeLandmarks eye(com.periomeasure.core.Eye eye) {
        return eye == com.periomeasure.core.Eye.OD ? od : os;
    }
}
