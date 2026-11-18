package com.periomeasure.core;

/** When the photograph was taken relative to surgery. */
public enum Timing {
    PRE_OP("PRE"),
    POST_OP("POST");

    /** Short code used in CSV and file names. */
    public final String code;

    Timing(String code) {
        this.code = code;
    }
}
