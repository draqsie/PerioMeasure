package com.periomeasure.io;

/** Key-value persistence for {@link Settings} (ImageJ Prefs in Fiji, a map in tests). */
public interface PrefsStore {

    String get(String key, String defaultValue);

    void set(String key, String value);
}
