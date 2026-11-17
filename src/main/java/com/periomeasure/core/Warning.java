package com.periomeasure.core;

import java.util.Locale;
import java.util.Objects;

/**
 * A warning raised during calibration or measurement. Carries a code (texts
 * are translated in the UI layer), the eye it concerns (null if not
 * eye-specific) and the offending value (unit depends on the code; NaN if
 * not applicable).
 */
public final class Warning {

    public final WarningCode code;
    public final Eye eye;
    public final double value;

    public Warning(WarningCode code, Eye eye, double value) {
        this.code = Objects.requireNonNull(code, "code");
        this.eye = eye;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Warning)) {
            return false;
        }
        Warning w = (Warning) o;
        return code == w.code && eye == w.eye && Double.compare(value, w.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, eye, value);
    }

    @Override
    public String toString() {
        return code + (eye == null ? "" : "[" + eye + "]") + String.format(Locale.ROOT, "(%.6g)", value);
    }
}
