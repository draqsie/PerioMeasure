package com.periomeasure.ui;

import com.periomeasure.io.PrefsStore;

import ij.Prefs;

/** Stores settings in ImageJ's preferences file (IJ_Prefs.txt). */
public final class ImageJPrefsStore implements PrefsStore {

    @Override
    public String get(String key, String defaultValue) {
        return Prefs.get(key, defaultValue);
    }

    @Override
    public void set(String key, String value) {
        Prefs.set(key, value);
    }
}
