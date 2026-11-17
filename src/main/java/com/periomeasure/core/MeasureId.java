package com.periomeasure.core;

/**
 * Every measurement PerioMeasure reports, in display/CSV priority order
 * (PFI, ICD, PFW, PFH first). Exact definitions: rules.md section 4.
 */
public enum MeasureId {
    /** Palpebral fissure index = PFH / PFW. */
    PFI(Unit.RATIO, false),
    /** Intercanthal distance |MC_OD - MC_OS|. */
    ICD(Unit.MM, true),
    /** Palpebral fissure width |LC - MC|. */
    PFW(Unit.MM, false),
    /** Palpebral fissure height (upper - lower) . v. */
    PFH(Unit.MM, false),
    /** Margin-reflex distance 1 = (upper - pupil) . v (signed). */
    MRD1(Unit.MM, false),
    /** Margin-reflex distance 2 = (pupil - lower) . v. */
    MRD2(Unit.MM, false),
    /** Canthal angle: atan2(d . v, d . lateral), d = LC - MC; positive if LC is higher. */
    CANTHAL_ANGLE(Unit.DEGREE, false),
    /** Interpupillary distance |P_OD - P_OS|. */
    IPD(Unit.MM, true),
    /** Palpebral fissure area (shoelace of the contour polygon). */
    FISSURE_AREA(Unit.MM2, false),
    /** Tarsal platform show = (crease - upper lid margin) . v. */
    TPS(Unit.MM, false),
    /** Brow height = (lower brow border - pupil) . v. */
    BROW_HEIGHT(Unit.MM, false),
    /** Superior scleral show = (upper lid margin - upper limbus) . v; negative = lid covers limbus. */
    SUPERIOR_SCLERAL_SHOW(Unit.MM, false),
    /** Inferior scleral show = (lower limbus - lower lid margin) . v; negative = lid covers limbus. */
    INFERIOR_SCLERAL_SHOW(Unit.MM, false);

    public final Unit unit;
    /** True for measurements between the two eyes (ICD, IPD). */
    public final boolean bilateral;

    MeasureId(Unit unit, boolean bilateral) {
        this.unit = unit;
        this.bilateral = bilateral;
    }
}
