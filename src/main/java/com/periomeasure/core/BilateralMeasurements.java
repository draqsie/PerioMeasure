package com.periomeasure.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Measurements of both eyes plus inter-eye distances and right-left differences. */
public final class BilateralMeasurements {

    public final EyeMeasurements od;
    public final EyeMeasurements os;
    public final Quantity icd;
    public final Quantity ipd;
    public final ReferenceAxes axes;
    private final Map<MeasureId, Quantity> differences;
    private final List<Warning> warnings;

    BilateralMeasurements(EyeMeasurements od, EyeMeasurements os, Quantity icd, Quantity ipd, ReferenceAxes axes,
            Map<MeasureId, Quantity> differences, List<Warning> warnings) {
        this.od = od;
        this.os = os;
        this.icd = icd;
        this.ipd = ipd;
        this.axes = axes;
        this.differences = Collections.unmodifiableMap(new EnumMap<>(differences));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /** @return OD minus OS for a per-eye measurement present in both eyes, else null */
    public Quantity difference(MeasureId id) {
        return differences.get(id);
    }

    public Map<MeasureId, Quantity> differences() {
        return differences;
    }

    /** @return image-level warnings followed by OD and OS warnings */
    public List<Warning> warnings() {
        return warnings;
    }
}
