package com.periomeasure.core;

import java.util.Locale;
import java.util.Objects;

/**
 * A warning raised during calibration or measurement. Carries a code (texts
 * are translated in the UI layer), the eye it concerns (null if not
 * eye-specific), the offending value (unit depends on the code; NaN if not
 * applicable) and an optional text detail (null if not applicable).
 */
public final class Warning {

    public final WarningCode code;
    public final Eye eye;
    public final double value;
    public final String detail;

    public Warning(WarningCode code, Eye eye, double value) {
        this(code, eye, value, null);
    }

    public Warning(WarningCode code, Eye eye, double value, String detail) {
        this.code = Objects.requireNonNull(code, "code");
        this.eye = eye;
        this.value = value;
        this.detail = detail;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Warning)) {
            return false;
        }
        Warning w = (Warning) o;
        return code == w.code && eye == w.eye && Double.compare(value, w.value) == 0
                && Objects.equals(detail, w.detail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, eye, value, detail);
    }

    /** Machine-readable form used in the CSV, e.g. {@code OBLIQUE_VIEW(0.95)} or {@code CANTHUS_ORDER[OD](-1.2)}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(code.name());
        if (eye != null) {
            sb.append('[').append(eye).append(']');
        }
        if (!Double.isNaN(value)) {
            sb.append(String.format(Locale.ROOT, "(%.6g)", value));
        }
        if (detail != null) {
            sb.append('{').append(detail).append('}');
        }
        return sb.toString();
    }
}
