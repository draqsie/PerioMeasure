package com.periomeasure.i18n;

/** User-interface language. Turkish is the default. */
public enum Language {
    TR("tr"),
    EN("en");

    public static final Language DEFAULT = TR;

    private final String code;

    Language(String code) {
        this.code = code;
    }

    /** @return the prefix used in {@code messages.properties}, e.g. {@code tr} */
    public String code() {
        return code;
    }

    /** @return the language for {@code code}, or {@link #DEFAULT} if unknown/null */
    public static Language fromCode(String code) {
        if (code != null) {
            for (Language l : values()) {
                if (l.code.equalsIgnoreCase(code.trim())) {
                    return l;
                }
            }
        }
        return DEFAULT;
    }
}
