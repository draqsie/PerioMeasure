package com.periomeasure.io;

/** Colour theme of the window. */
public enum Theme {
    LIGHT("light"),
    DARK("dark");

    /** Value stored in the preferences. */
    public final String code;

    Theme(String code) {
        this.code = code;
    }

    /** @return the theme for {@code code}, LIGHT if unknown or null */
    public static Theme fromCode(String code) {
        if (code != null) {
            for (Theme t : values()) {
                if (t.code.equalsIgnoreCase(code.trim())) {
                    return t;
                }
            }
        }
        return LIGHT;
    }
}
