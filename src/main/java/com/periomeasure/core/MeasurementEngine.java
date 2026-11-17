package com.periomeasure.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes every measurement from landmarks (px), reference axes and the
 * image scale. Definitions follow rules.md section 4 exactly:
 * <pre>
 *   PFW  = |LC - MC|
 *   PFH  = (Upper - Lower) . v
 *   MRD1 = (Upper - Pupil) . v          (signed)
 *   MRD2 = (Pupil - Lower) . v
 *   PFI  = PFH / PFW
 *   canthal angle = atan2(d . v, d . lateral),  d = LC - MC
 *   ICD  = |MC_OD - MC_OS|,  IPD = |P_OD - P_OS|
 * </pre>
 * Uncertainty (1 sigma): lengths get {@code sigmaClick = 1 px x mm/px} and
 * {@code sigmaCal = |L| x relative scale sigma}; ratios and angles have no
 * calibration component because the scale cancels.
 */
public final class MeasurementEngine {

    /** Assumed click precision (px) for every landmark. */
    public static final double CLICK_UNCERTAINTY_PX = 1.0;
    /** Tolerance for |MRD1 + MRD2 - PFH| (mm). */
    public static final double MRD_CONSISTENCY_TOLERANCE_MM = 0.3;
    /** Maximum horizontal distance of a lid point from the pupil's vertical line (mm). */
    public static final double LID_OFF_AXIS_TOLERANCE_MM = 1.0;

    private MeasurementEngine() {
    }

    /**
     * Measures one eye.
     *
     * @throws IllegalArgumentException if the canthi coincide
     */
    public static EyeMeasurements measureEye(Eye eye, EyeLandmarks lm, ReferenceAxes axes, Scale scale) {
        Objects.requireNonNull(eye, "eye");
        Objects.requireNonNull(lm, "landmarks");
        Objects.requireNonNull(axes, "axes");
        Objects.requireNonNull(scale, "scale");

        double s = scale.mmPerPx;
        double rel = scale.relativeSigma;
        double clickMm = CLICK_UNCERTAINTY_PX * s;
        Vec2 u = axes.u;
        Vec2 v = axes.v;
        Vec2 lateral = axes.lateral(eye);

        Map<MeasureId, Quantity> out = new EnumMap<>(MeasureId.class);
        List<Warning> warnings = new ArrayList<>();

        Vec2 d = lm.lateralCanthus.minus(lm.medialCanthus);
        double pfwPx = d.norm();
        if (pfwPx == 0) {
            throw new IllegalArgumentException(eye + ": medial and lateral canthus coincide");
        }
        double pfhPx = lm.upperLid.minus(lm.lowerLid).dot(v);
        double mrd1Px = lm.upperLid.minus(lm.pupil).dot(v);
        double mrd2Px = lm.pupil.minus(lm.lowerLid).dot(v);

        Quantity pfw = length(pfwPx, s, rel);
        Quantity pfh = length(pfhPx, s, rel);
        out.put(MeasureId.PFW, pfw);
        out.put(MeasureId.PFH, pfh);
        out.put(MeasureId.MRD1, length(mrd1Px, s, rel));
        out.put(MeasureId.MRD2, length(mrd2Px, s, rel));

        // PFI = H / W;  dPFI/dH = 1/W,  dPFI/dW = -H/W^2.
        double h = pfh.value;
        double w = pfw.value;
        double pfiClick = Math.sqrt(sq(clickMm / w) + sq(h * clickMm / (w * w)));
        out.put(MeasureId.PFI, new Quantity(h / w, pfiClick, 0, Unit.RATIO));

        double angle = Math.toDegrees(Math.atan2(d.dot(v), d.dot(lateral)));
        double angleClick = Math.toDegrees(Math.atan(CLICK_UNCERTAINTY_PX / pfwPx));
        out.put(MeasureId.CANTHAL_ANGLE, new Quantity(angle, angleClick, 0, Unit.DEGREE));

        if (lm.contour != null) {
            double areaPx = Polygons.area(lm.contour);
            double area = areaPx * s * s;
            double areaClick = Polygons.perimeter(lm.contour) * CLICK_UNCERTAINTY_PX * s * s;
            out.put(MeasureId.FISSURE_AREA, new Quantity(area, areaClick, 2 * area * rel, Unit.MM2));
        }
        if (lm.upperLidCrease != null) {
            out.put(MeasureId.TPS, length(lm.upperLidCrease.minus(lm.upperLid).dot(v), s, rel));
        }
        if (lm.browLowerBorder != null) {
            out.put(MeasureId.BROW_HEIGHT, length(lm.browLowerBorder.minus(lm.pupil).dot(v), s, rel));
        }
        if (lm.limbusUpper != null) {
            out.put(MeasureId.SUPERIOR_SCLERAL_SHOW, length(lm.upperLid.minus(lm.limbusUpper).dot(v), s, rel));
        }
        if (lm.limbusLower != null) {
            out.put(MeasureId.INFERIOR_SCLERAL_SHOW, length(lm.limbusLower.minus(lm.lowerLid).dot(v), s, rel));
        }

        // ---- warnings
        double lateralExtent = d.dot(lateral);
        if (lateralExtent <= 0) {
            warnings.add(new Warning(WarningCode.CANTHUS_ORDER, eye, lateralExtent * s));
        }
        double mrdMismatch = Math.abs((mrd1Px + mrd2Px - pfhPx) * s);
        if (mrdMismatch > MRD_CONSISTENCY_TOLERANCE_MM) {
            warnings.add(new Warning(WarningCode.MRD_INCONSISTENT, eye, mrdMismatch));
        }
        if (pfhPx <= 0) {
            warnings.add(new Warning(WarningCode.NONPOSITIVE_PFH, eye, pfh.value));
        }
        double offAxis = Math.max(Math.abs(lm.upperLid.minus(lm.pupil).dot(u)),
                Math.abs(lm.lowerLid.minus(lm.pupil).dot(u))) * s;
        if (offAxis > LID_OFF_AXIS_TOLERANCE_MM) {
            warnings.add(new Warning(WarningCode.LID_POINT_OFF_PUPIL_AXIS, eye, offAxis));
        }

        return new EyeMeasurements(eye, out, warnings);
    }

    /**
     * Measures both eyes with axes from the pupil centres, plus ICD, IPD and
     * OD - OS differences.
     */
    public static BilateralMeasurements measureBoth(EyeLandmarks od, EyeLandmarks os, Scale scale) {
        Objects.requireNonNull(od, "od");
        Objects.requireNonNull(os, "os");
        Objects.requireNonNull(scale, "scale");
        ReferenceAxes axes = ReferenceAxes.fromPupils(od.pupil, os.pupil);
        EyeMeasurements mOd = measureEye(Eye.OD, od, axes, scale);
        EyeMeasurements mOs = measureEye(Eye.OS, os, axes, scale);

        double s = scale.mmPerPx;
        double rel = scale.relativeSigma;
        Quantity icd = length(od.medialCanthus.distanceTo(os.medialCanthus), s, rel);
        Quantity ipd = length(od.pupil.distanceTo(os.pupil), s, rel);

        Map<MeasureId, Quantity> diffs = new EnumMap<>(MeasureId.class);
        for (MeasureId id : MeasureId.values()) {
            Quantity a = mOd.get(id);
            Quantity b = mOs.get(id);
            if (a == null || b == null) {
                continue;
            }
            double diff = a.value - b.value;
            // One shared scale: its error is fully correlated between the eyes.
            double calPower = id.unit == Unit.MM ? 1 : id.unit == Unit.MM2 ? 2 : 0;
            diffs.put(id, new Quantity(diff, Math.hypot(a.sigmaClick, b.sigmaClick),
                    calPower * Math.abs(diff) * rel, id.unit));
        }

        List<Warning> warnings = new ArrayList<>();
        if (axes.isMirrored()) {
            warnings.add(new Warning(WarningCode.IMAGE_MIRRORED, null, Double.NaN));
        }
        warnings.addAll(mOd.warnings());
        warnings.addAll(mOs.warnings());
        return new BilateralMeasurements(mOd, mOs, icd, ipd, axes, diffs, warnings);
    }

    private static Quantity length(double px, double mmPerPx, double rel) {
        double mm = px * mmPerPx;
        return new Quantity(mm, CLICK_UNCERTAINTY_PX * mmPerPx, Math.abs(mm) * rel, Unit.MM);
    }

    private static double sq(double x) {
        return x * x;
    }
}
