package com.periomeasure.ui;

import java.awt.EventQueue;

import com.periomeasure.io.Settings;

import ij.plugin.PlugIn;

/** Plugins &gt; PerioMeasure &gt; Settings... */
public class SettingsPlugin implements PlugIn {

    @Override
    public void run(String arg) {
        EventQueue.invokeLater(() -> {
            Settings current = Settings.load(new ImageJPrefsStore());
            Themes.apply(current.theme);
            Settings updated = SettingsDialog.show(null, current);
            if (updated != null) {
                PerioMeasureApp.settingsChanged(updated);
            }
        });
    }
}
