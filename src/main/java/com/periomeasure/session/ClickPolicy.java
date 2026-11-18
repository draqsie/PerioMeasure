package com.periomeasure.session;

/**
 * One click places a point; pressing on an existing point drags it.
 * A step advances by itself as soon as it holds all the points it can take
 * (one for most landmarks, two for the reference line); open-ended steps
 * (sticker edge, contour) wait for the user to continue.
 */
public final class ClickPolicy {

    public enum Action {
        /** Place (or replace) a point of the current step. */
        PLACE,
        /** Drag the existing point under the cursor. */
        DRAG
    }

    private ClickPolicy() {
    }

    public static Action decide(boolean onExistingPoint) {
        return onExistingPoint ? Action.DRAG : Action.PLACE;
    }

    public static boolean advanceAfterPlacing(Landmark l, int pointsInStep) {
        return pointsInStep >= l.maxPoints;
    }
}
