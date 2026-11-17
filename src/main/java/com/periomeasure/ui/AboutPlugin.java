package com.periomeasure.ui;

import com.periomeasure.Version;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

import ij.IJ;
import ij.plugin.PlugIn;

/** Plugins &gt; PerioMeasure &gt; Hakkında / About — shows version and disclaimer. */
public class AboutPlugin implements PlugIn {

    @Override
    public void run(String arg) {
        Language lang = Language.DEFAULT; // Phase 2: read from user settings
        IJ.showMessage(title(lang), body(lang));
    }

    static String title(Language lang) {
        return Messages.get(lang, Msg.ABOUT_TITLE);
    }

    static String body(Language lang) {
        return Messages.get(lang, Msg.ABOUT_BODY, Version.get());
    }
}
