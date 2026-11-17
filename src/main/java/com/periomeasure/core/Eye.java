package com.periomeasure.core;

/** OD = right eye, OS = left eye (patient's anatomy). */
public enum Eye {
    OD,
    OS;

    /**
     * The horizontal axis u points from the OD pupil to the OS pupil, i.e.
     * towards the patient's LEFT. Lateral for the right eye is therefore -u,
     * and for the left eye +u.
     *
     * @return -1 for OD, +1 for OS
     */
    public int lateralSign() {
        return this == OD ? -1 : 1;
    }
}
