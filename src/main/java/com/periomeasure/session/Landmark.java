package com.periomeasure.session;

/** What the user clicks in one step. */
public enum Landmark {
    /** Points on the sticker edge (at least 8). */
    CALIBRATION_EDGE(Calibration8.MIN, Integer.MAX_VALUE, false),
    /** Two points defining the horizontal (single-eye sessions only). */
    REFERENCE_LINE(2, 2, false),
    PUPIL(1, 1, true),
    UPPER_LID(1, 1, true),
    LOWER_LID(1, 1, true),
    MEDIAL_CANTHUS(1, 1, true),
    LATERAL_CANTHUS(1, 1, true),
    UPPER_LID_CREASE(1, 1, true),
    BROW_LOWER_BORDER(1, 1, true),
    LIMBUS_UPPER(1, 1, true),
    LIMBUS_LOWER(1, 1, true),
    /** Palpebral fissure contour polygon (at least 3 points). */
    CONTOUR(3, Integer.MAX_VALUE, true);

    public final int minPoints;
    public final int maxPoints;
    /** True if the landmark belongs to one eye. */
    public final boolean eyeSpecific;

    Landmark(int minPoints, int maxPoints, boolean eyeSpecific) {
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.eyeSpecific = eyeSpecific;
    }

    public boolean isMultiPoint() {
        return maxPoints > 1;
    }

    /** Keeps the calibration minimum in one place (core). */
    private static final class Calibration8 {
        static final int MIN = com.periomeasure.core.Calibration.MIN_MANUAL_POINTS;
    }
}
