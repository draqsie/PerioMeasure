package com.periomeasure.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Which eye(s) are measured in a session. */
public enum EyeSelection {
    BOTH(Eye.OD, Eye.OS),
    OD(Eye.OD),
    OS(Eye.OS);

    private final List<Eye> eyes;

    EyeSelection(Eye... eyes) {
        this.eyes = Collections.unmodifiableList(Arrays.asList(eyes));
    }

    /** @return the measured eyes, OD first */
    public List<Eye> eyes() {
        return eyes;
    }
}
