package com.periomeasure.core;

/** The points do not determine an ellipse (collinear, coincident, or fit did not converge). */
public class EllipseFitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EllipseFitException(String message) {
        super(message);
    }
}
