package com.periomeasure.session;

import com.periomeasure.synthetic.SyntheticScene;

/** Shares session test helpers with tests in other packages. */
public final class SessionTestAccess {

    private SessionTestAccess() {
    }

    /** Places every landmark of every step at its exact synthetic position, ending on the last step. */
    public static void clickAll(MeasurementSession s, SyntheticScene scene) {
        MeasurementSessionTest.clickAll(s, scene);
    }

    public static SessionSetup setup(com.periomeasure.core.EyeSelection eyes, AxisMode axis,
            OptionalLandmark... optional) {
        return MeasurementSessionTest.setup(eyes, axis, optional);
    }
}
