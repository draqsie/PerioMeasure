package com.periomeasure.session;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.Timing;

/** What the user entered before measuring (validated). */
public final class SessionSetup {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    /** Study code of the patient (never a name). */
    public final String patientId;
    public final LocalDate photoDate;
    public final Timing timing;
    public final EyeSelection eyes;
    /** Horizontal reference for single-eye sessions; ignored for both eyes. */
    public final AxisMode axisMode;
    public final Set<OptionalLandmark> optional;

    /** @throws IllegalArgumentException if the patient code is blank */
    public SessionSetup(String patientId, LocalDate photoDate, Timing timing, EyeSelection eyes, AxisMode axisMode,
            Set<OptionalLandmark> optional) {
        if (patientId == null || patientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient code must not be empty");
        }
        this.patientId = patientId.trim();
        this.photoDate = Objects.requireNonNull(photoDate, "photoDate");
        this.timing = Objects.requireNonNull(timing, "timing");
        this.eyes = Objects.requireNonNull(eyes, "eyes");
        this.axisMode = Objects.requireNonNull(axisMode, "axisMode");
        EnumSet<OptionalLandmark> copy = EnumSet.noneOf(OptionalLandmark.class);
        copy.addAll(optional);
        this.optional = Collections.unmodifiableSet(copy);
    }

    /**
     * Parses a date in the strict form YYYY-MM-DD (e.g. 2026-03-02).
     *
     * @throws IllegalArgumentException for any other form or an impossible date
     */
    public static LocalDate parseDate(String s) {
        String t = s == null ? "" : s.trim();
        if (!t.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Date must be YYYY-MM-DD: " + s);
        }
        try {
            return LocalDate.parse(t, DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + s, e);
        }
    }
}
