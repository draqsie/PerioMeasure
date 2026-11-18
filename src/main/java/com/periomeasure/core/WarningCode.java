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
    LID_POINT_OFF_PUPIL_AXIS,
    /** Image narrower than 2000 px. value = width in px. */
    IMAGE_TOO_NARROW,
    /** Estimated JPEG quality below the threshold. value = estimated IJG quality (1-100). */
    JPEG_LOW_QUALITY,
    /** The image file carries its own spatial calibration, which is ignored. value = NaN. */
    FILE_CALIBRATION_IGNORED,
    /** The photograph is in a cloud-synchronised folder. detail = provider name. */
    CLOUD_FOLDER_INPUT,
    /** The output folder is cloud-synchronised. detail = provider name. */
    CLOUD_FOLDER_OUTPUT,
    /** The photo was shown rotated/mirrored as its EXIF orientation requests. value = EXIF orientation (2-8). */
    EXIF_ORIENTATION_APPLIED
}
