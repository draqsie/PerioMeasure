package com.periomeasure.ui;

import com.periomeasure.io.Settings;

import ij.plugin.PlugIn;

/** Plugins &gt; PerioMeasure &gt; Settings... */
public class SettingsPlugin implements PlugIn {

    @Override
    public void run(String arg) {
        Settings updated = SettingsDialog.show(Settings.load(new ImageJPrefsStore()));
        if (updated != null) {
            PerioMeasureApp.settingsChanged(updated);
        }
    }
}
