package com.periomeasure.session;

/** Reference to one placed point: (step, index within the step). */
public final class Handle {

    public final int stepIndex;
    public final int pointIndex;

    public Handle(int stepIndex, int pointIndex) {
        this.stepIndex = stepIndex;
        this.pointIndex = pointIndex;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Handle && ((Handle) o).stepIndex == stepIndex && ((Handle) o).pointIndex == pointIndex;
    }

    @Override
    public int hashCode() {
        return 31 * stepIndex + pointIndex;
    }
}
