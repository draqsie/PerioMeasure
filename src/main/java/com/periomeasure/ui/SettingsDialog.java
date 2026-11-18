package com.periomeasure.ui;

import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.io.PrefsStore;
import com.periomeasure.io.Settings;

import ij.IJ;
import ij.Prefs;
import ij.gui.GenericDialog;

/** Settings dialog; validated values are saved to the ImageJ preferences. */
final class SettingsDialog {

    private SettingsDialog() {
    }

    /** @return the new settings (already saved), or null if cancelled */
    static Settings show(Settings current) {
        PrefsStore store = new ImageJPrefsStore();
        Settings s = current;
        while (true) {
            Language l = s.language;
            String[] langs = {"Türkçe", "English"};
            String[] decimals = {Messages.get(l, Msg.DEC_DOT), Messages.get(l, Msg.DEC_COMMA)};
            GenericDialog gd = new GenericDialog(Messages.get(l, Msg.SET_TITLE));
            gd.addChoice(Messages.get(l, Msg.SET_LANGUAGE), langs, langs[l == Language.TR ? 0 : 1]);
            gd.addNumericField(Messages.get(l, Msg.SET_DIAMETER), s.referenceDiameterMm, 2);
            gd.addChoice(Messages.get(l, Msg.SET_DECIMAL), decimals, decimals[s.decimalSeparator == ',' ? 1 : 0]);
            gd.addDirectoryField(Messages.get(l, Msg.SET_OUTPUT), s.outputDir, 30);
            gd.addSlider(Messages.get(l, Msg.SET_ZOOM), Settings.MIN_ZOOM, Settings.MAX_ZOOM, s.zoomFactor, 1);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return null;
            }
            Language newLang = gd.getNextChoiceIndex() == 0 ? Language.TR : Language.EN;
            double diameter = gd.getNextNumber();
            boolean badNumber = gd.invalidNumber();
            char dec = gd.getNextChoiceIndex() == 1 ? ',' : '.';
            String out = gd.getNextString();
            int zoom = (int) Math.round(gd.getNextNumber());
            try {
                if (badNumber) {
                    throw new IllegalArgumentException("diameter");
                }
                Settings updated = s.withLanguage(newLang).withReferenceDiameterMm(diameter).withDecimalSeparator(dec)
                        .withOutputDir(out)
                        .withZoomFactor(Math.max(Settings.MIN_ZOOM, Math.min(Settings.MAX_ZOOM, zoom)));
                updated.save(store);
                Prefs.savePreferences();
                return updated;
            } catch (IllegalArgumentException e) {
                IJ.error(Messages.get(l, Msg.ERR_TITLE), Messages.get(l, Msg.ERR_DIAMETER));
                s = s.withLanguage(newLang);
            }
        }
    }
}
