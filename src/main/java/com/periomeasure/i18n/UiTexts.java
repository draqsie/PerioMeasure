package com.periomeasure.i18n;

import com.periomeasure.core.Eye;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.ReferenceAxes;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Warning;
import com.periomeasure.io.Formats;
import com.periomeasure.session.Landmark;

/**
 * User-facing texts for domain enums. Lookups go by naming convention
 * ({@code STEP_<LANDMARK>_TITLE}, {@code MEASURE_<ID>}, {@code WARN_<CODE>}, ...);
 * {@code UiTextsTest} checks that every enum constant has its texts.
 */
public final class UiTexts {

    private UiTexts() {
    }

    public static String stepTitle(Language l, Landmark m) {
        return Messages.get(l, Msg.valueOf("STEP_" + m.name() + "_TITLE"));
    }

    public static String stepHelp(Language l, Landmark m) {
        return Messages.get(l, Msg.valueOf("STEP_" + m.name() + "_HELP"));
    }

    public static String measureLabel(Language l, MeasureId id) {
        return Messages.get(l, Msg.valueOf("MEASURE_" + id.name()));
    }

    /** @return the translated warning; {0} = value, {1} = eye (OD/OS), {2} = detail */
    public static String warning(Language l, Warning w) {
        String value = "";
        if (!Double.isNaN(w.value)) {
            switch (w.code) {
                case OBLIQUE_VIEW:
                    value = Formats.number(w.value, 3, l);
                    break;
                case LOW_RESOLUTION:
                case IMAGE_TOO_NARROW:
                case JPEG_LOW_QUALITY:
                    value = Formats.number(w.value, 0, l);
                    break;
                default:
                    value = Formats.number(w.value, 2, l);
            }
        }
        return Messages.get(l, Msg.valueOf("WARN_" + w.code.name()), value,
                w.eye == null ? "" : w.eye.name(), w.detail == null ? "" : w.detail);
    }

    public static String eye(Language l, Eye e) {
        return Messages.get(l, Msg.valueOf("EYE_" + e.name()));
    }

    public static String timing(Language l, Timing t) {
        return Messages.get(l, Msg.valueOf("TIMING_" + t.code));
    }

    public static String axisSource(Language l, ReferenceAxes.Source s) {
        return Messages.get(l, Msg.valueOf("AXIS_SRC_" + s.name()));
    }
}
