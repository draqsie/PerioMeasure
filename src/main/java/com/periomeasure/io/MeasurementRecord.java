package com.periomeasure.io;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.periomeasure.core.BilateralMeasurements;
import com.periomeasure.core.Calibration;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeMeasurements;
import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.ReferenceAxes;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Warning;

/** Everything that is saved and shown for one measured photograph. */
public final class MeasurementRecord {

    public final String pluginVersion;
    public final LocalDateTime measuredAt;
    public final String patientId;
    public final Timing timing;
    public final LocalDate photoDate;
    /** SHA-256 of the photo's file name, or "" if the image has no file. */
    public final String imageFileSha256;
    public final int imageWidth;
    public final int imageHeight;
    public final Calibration calibration;
    public final ReferenceAxes axes;
    public final EyeSelection eyes;
    /** Measured eyes (one or two entries). */
    public final Map<Eye, EyeMeasurements> perEye;
    /** Both-eye results, or null for a single-eye session. */
    public final BilateralMeasurements bilateral;
    /** Image-level warnings that are not part of the measurements (resolution, JPEG, cloud folder...). */
    public final List<Warning> imageWarnings;

    public MeasurementRecord(String pluginVersion, LocalDateTime measuredAt, String patientId, Timing timing,
            LocalDate photoDate, String imageFileSha256, int imageWidth, int imageHeight, Calibration calibration,
            ReferenceAxes axes, EyeSelection eyes, Map<Eye, EyeMeasurements> perEye,
            BilateralMeasurements bilateral, List<Warning> imageWarnings) {
        this.pluginVersion = Objects.requireNonNull(pluginVersion, "pluginVersion");
        this.measuredAt = Objects.requireNonNull(measuredAt, "measuredAt");
        this.patientId = Objects.requireNonNull(patientId, "patientId");
        this.timing = Objects.requireNonNull(timing, "timing");
        this.photoDate = Objects.requireNonNull(photoDate, "photoDate");
        this.imageFileSha256 = imageFileSha256 == null ? "" : imageFileSha256;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.calibration = Objects.requireNonNull(calibration, "calibration");
        this.axes = Objects.requireNonNull(axes, "axes");
        this.eyes = Objects.requireNonNull(eyes, "eyes");
        this.perEye = Collections.unmodifiableMap(new EnumMap<>(perEye));
        this.bilateral = bilateral;
        this.imageWarnings = Collections.unmodifiableList(new ArrayList<>(imageWarnings));
        for (Eye e : eyes.eyes()) {
            if (!this.perEye.containsKey(e)) {
                throw new IllegalArgumentException("Missing measurements for " + e);
            }
        }
        if (eyes == EyeSelection.BOTH && bilateral == null) {
            throw new IllegalArgumentException("Both-eye record needs bilateral measurements");
        }
    }

    /** @return calibration, image and measurement warnings in that order, without duplicates */
    public List<Warning> allWarnings() {
        List<Warning> all = new ArrayList<>(calibration.warnings());
        for (Warning w : imageWarnings) {
            if (!all.contains(w)) {
                all.add(w);
            }
        }
        List<Warning> measurement = bilateral != null ? bilateral.warnings() : new ArrayList<Warning>();
        if (bilateral == null) {
            for (EyeMeasurements m : perEye.values()) {
                measurement.addAll(m.warnings());
            }
        }
        for (Warning w : measurement) {
            if (!all.contains(w)) {
                all.add(w);
            }
        }
        return all;
    }
}
