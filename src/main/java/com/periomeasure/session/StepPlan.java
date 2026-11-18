package com.periomeasure.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeSelection;

/**
 * Builds the ordered list of steps:
 * <ol>
 *   <li>calibration sticker edge;</li>
 *   <li>reference line (single eye with a user-drawn horizontal only);</li>
 *   <li>the pupil of every measured eye (first, so the measurement axes are
 *       known and can be drawn while the lids are marked);</li>
 *   <li>per eye: upper lid, lower lid, medial canthus, lateral canthus,
 *       then the selected optional landmarks.</li>
 * </ol>
 */
public final class StepPlan {

    private StepPlan() {
    }

    public static List<Step> build(SessionSetup setup) {
        List<Step> s = new ArrayList<>();
        s.add(new Step(Landmark.CALIBRATION_EDGE, null, false));
        if (setup.eyes != EyeSelection.BOTH && setup.axisMode == AxisMode.USER_LINE) {
            s.add(new Step(Landmark.REFERENCE_LINE, null, false));
        }
        for (Eye eye : setup.eyes.eyes()) {
            s.add(new Step(Landmark.PUPIL, eye, false));
        }
        for (Eye eye : setup.eyes.eyes()) {
            s.add(new Step(Landmark.UPPER_LID, eye, false));
            s.add(new Step(Landmark.LOWER_LID, eye, false));
            s.add(new Step(Landmark.MEDIAL_CANTHUS, eye, false));
            s.add(new Step(Landmark.LATERAL_CANTHUS, eye, false));
            if (setup.optional.contains(OptionalLandmark.CREASE)) {
                s.add(new Step(Landmark.UPPER_LID_CREASE, eye, true));
            }
            if (setup.optional.contains(OptionalLandmark.BROW)) {
                s.add(new Step(Landmark.BROW_LOWER_BORDER, eye, true));
            }
            if (setup.optional.contains(OptionalLandmark.LIMBUS)) {
                s.add(new Step(Landmark.LIMBUS_UPPER, eye, true));
                s.add(new Step(Landmark.LIMBUS_LOWER, eye, true));
            }
            if (setup.optional.contains(OptionalLandmark.CONTOUR)) {
                s.add(new Step(Landmark.CONTOUR, eye, true));
            }
        }
        return Collections.unmodifiableList(s);
    }
}
