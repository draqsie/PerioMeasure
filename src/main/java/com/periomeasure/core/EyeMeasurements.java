package com.periomeasure.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Measurements of one eye. */
public final class EyeMeasurements {

    public final Eye eye;
    private final Map<MeasureId, Quantity> values;
    private final List<Warning> warnings;

    EyeMeasurements(Eye eye, Map<MeasureId, Quantity> values, List<Warning> warnings) {
        this.eye = eye;
        this.values = Collections.unmodifiableMap(new EnumMap<>(values));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public boolean has(MeasureId id) {
        return values.containsKey(id);
    }

    /** @return the measurement, or null if its landmarks were not marked */
    public Quantity get(MeasureId id) {
        return values.get(id);
    }

    /** @return all measurements present, in {@link MeasureId} order */
    public Map<MeasureId, Quantity> all() {
        return values;
    }

    public List<Warning> warnings() {
        return warnings;
    }
}
