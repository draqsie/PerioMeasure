package com.periomeasure.i18n;

/**
 * Every user-visible text key. Using an enum (instead of free strings) means a
 * typo in a key is a compile error, and {@code MessagesTest} guarantees each
 * constant has a non-empty text in every {@link Language}.
 */
public enum Msg {
    ABOUT_TITLE("about.title"),
    ABOUT_BODY("about.body");

    private final String key;

    Msg(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
