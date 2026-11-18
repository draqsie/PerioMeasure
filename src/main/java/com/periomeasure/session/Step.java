package com.periomeasure.session;

import com.periomeasure.core.Eye;

/** One step of a session: a landmark, for one eye (null for image-level steps). */
public final class Step {

    public final Landmark landmark;
    public final Eye eye;
    /** True if the user may skip this step. */
    public final boolean optional;

    public Step(Landmark landmark, Eye eye, boolean optional) {
        this.landmark = landmark;
        this.eye = eye;
        this.optional = optional;
    }

    @Override
    public String toString() {
        return eye == null ? landmark.name() : landmark.name() + "[" + eye + "]";
    }
}
