package com.periomeasure.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClickPolicyTest {

    @Test
    void pressOnAPointDragsItOtherwiseAClickPlaces() {
        assertEquals(ClickPolicy.Action.DRAG, ClickPolicy.decide(true));
        assertEquals(ClickPolicy.Action.PLACE, ClickPolicy.decide(false));
    }

    @Test
    void stepsAdvanceAutomaticallyOnceTheirLastPointIsPlaced() {
        assertTrue(ClickPolicy.advanceAfterPlacing(Landmark.PUPIL, 1));
        assertTrue(ClickPolicy.advanceAfterPlacing(Landmark.LATERAL_CANTHUS, 1));
        assertFalse(ClickPolicy.advanceAfterPlacing(Landmark.REFERENCE_LINE, 1));
        assertTrue(ClickPolicy.advanceAfterPlacing(Landmark.REFERENCE_LINE, 2));
        // open-ended landmarks wait for the user to press "Continue"
        assertFalse(ClickPolicy.advanceAfterPlacing(Landmark.CALIBRATION_EDGE, 30));
        assertFalse(ClickPolicy.advanceAfterPlacing(Landmark.CONTOUR, 12));
    }
}
