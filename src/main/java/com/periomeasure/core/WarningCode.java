package com.periomeasure.core;

/**
 * Machine-readable warning codes; user-facing texts live in the i18n layer.
 * The meaning of {@link Warning#value} is given per code.
 */
public enum WarningCode {
    /** Sticker minor/major axis ratio below 0.97. value = axis ratio. */
    OBLIQUE_VIEW,
    /** Sticker diameter below 150 px. value = major axis in px. */
    LOW_RESOLUTION,
    /** OD pupil lies to the image right of the OS pupil. value = NaN. */
    IMAGE_MIRRORED,
    /** Lateral canthus is not lateral of the medial canthus. value = (LC - MC) . lateral in mm. */
    CANTHUS_ORDER,
    /** |MRD1 + MRD2 - PFH| above 0.3 mm (numerical guard). value = difference in mm. */
    MRD_INCONSISTENT,
    /** Upper lid margin not above the lower lid margin. value = PFH in mm. */
    NONPOSITIVE_PFH,
    /** A lid margin point is more than 1 mm from the pupil's vertical line. value = offset in mm. */
    LID_POINT_OFF_PUPIL_AXIS
}
