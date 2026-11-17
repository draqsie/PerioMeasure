package com.periomeasure.synthetic;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.MeasureId;

/**
 * Expected measurements computed directly from the face model in the
 * millimetre face frame (X to the patient's left, Y up), WITHOUT using the
 * plugin's measurement engine. This is the independent oracle for the
 * end-to-end tests.
 */
public final class GroundTruth {

    private final Map<Eye, Map<MeasureId, Double>> perEye = new EnumMap<>(Eye.class);
    private final double icd;
    private final double ipd;

    private GroundTruth(FaceModel f) {
        // Horizontal axis: right pupil -> left pupil. Up axis: rotate u by +90 deg (Y is up here).
        double ux = f.os.pupil.x - f.od.pupil.x;
        double uy = f.os.pupil.y - f.od.pupil.y;
        double un = Math.hypot(ux, uy);
        ux /= un;
        uy /= un;
        double vx = -uy;
        double vy = ux;
        for (Eye eye : Eye.values()) {
            EyeLandmarks e = f.eye(eye);
            double lx = eye == Eye.OD ? -ux : ux; // lateral = away from the nose
            double ly = eye == Eye.OD ? -uy : uy;
            Map<MeasureId, Double> m = new EnumMap<>(MeasureId.class);
            double dx = e.lateralCanthus.x - e.medialCanthus.x;
            double dy = e.lateralCanthus.y - e.medialCanthus.y;
            double pfw = Math.hypot(dx, dy);
            double pfh = (e.upperLid.x - e.lowerLid.x) * vx + (e.upperLid.y - e.lowerLid.y) * vy;
            m.put(MeasureId.PFW, pfw);
            m.put(MeasureId.PFH, pfh);
            m.put(MeasureId.MRD1, (e.upperLid.x - e.pupil.x) * vx + (e.upperLid.y - e.pupil.y) * vy);
            m.put(MeasureId.MRD2, (e.pupil.x - e.lowerLid.x) * vx + (e.pupil.y - e.lowerLid.y) * vy);
            m.put(MeasureId.PFI, pfh / pfw);
            m.put(MeasureId.CANTHAL_ANGLE, Math.toDegrees(Math.atan2(dx * vx + dy * vy, dx * lx + dy * ly)));
            m.put(MeasureId.FISSURE_AREA, shoelace(e.contour));
            m.put(MeasureId.TPS, (e.upperLidCrease.x - e.upperLid.x) * vx + (e.upperLidCrease.y - e.upperLid.y) * vy);
            m.put(MeasureId.BROW_HEIGHT,
                    (e.browLowerBorder.x - e.pupil.x) * vx + (e.browLowerBorder.y - e.pupil.y) * vy);
            m.put(MeasureId.SUPERIOR_SCLERAL_SHOW,
                    (e.upperLid.x - e.limbusUpper.x) * vx + (e.upperLid.y - e.limbusUpper.y) * vy);
            m.put(MeasureId.INFERIOR_SCLERAL_SHOW,
                    (e.limbusLower.x - e.lowerLid.x) * vx + (e.limbusLower.y - e.lowerLid.y) * vy);
            perEye.put(eye, Collections.unmodifiableMap(m));
        }
        icd = Math.hypot(f.od.medialCanthus.x - f.os.medialCanthus.x, f.od.medialCanthus.y - f.os.medialCanthus.y);
        ipd = Math.hypot(f.od.pupil.x - f.os.pupil.x, f.od.pupil.y - f.os.pupil.y);
    }

    public static GroundTruth of(FaceModel face) {
        return new GroundTruth(face);
    }

    /** @return expected per-eye values (mm, mm^2, ratio or degrees) */
    public Map<MeasureId, Double> perEye(Eye eye) {
        return perEye.get(eye);
    }

    public double icd() {
        return icd;
    }

    public double ipd() {
        return ipd;
    }

    private static double shoelace(List<com.periomeasure.core.Vec2> p) {
        double twice = 0;
        for (int i = 0; i < p.size(); i++) {
            com.periomeasure.core.Vec2 a = p.get(i);
            com.periomeasure.core.Vec2 b = p.get((i + 1) % p.size());
            twice += a.x * b.y - b.x * a.y;
        }
        return Math.abs(twice) / 2;
    }
}
