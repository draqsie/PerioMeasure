package com.periomeasure.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Eye;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.ReferenceAxes;
import com.periomeasure.core.Timing;
import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;
import com.periomeasure.session.Landmark;

class UiTextsTest {

    private static void assertText(String s) {
        assertTrue(s != null && !s.trim().isEmpty(), "empty text");
        assertFalse(s.contains("!"), "missing key: " + s);
        assertFalse(s.matches(".*\\{\\d}.*"), "unreplaced placeholder: " + s);
    }

    @Test
    void everyLandmarkHasTitleAndHelpInEveryLanguage() {
        for (Language l : Language.values()) {
            for (Landmark m : Landmark.values()) {
                assertText(UiTexts.stepTitle(l, m));
                assertText(UiTexts.stepHelp(l, m));
            }
        }
    }

    @Test
    void everyMeasurementHasALabel() {
        for (Language l : Language.values()) {
            for (MeasureId id : MeasureId.values()) {
                assertText(UiTexts.measureLabel(l, id));
            }
        }
        assertEquals("PFI (palpebral fissür indeksi)", UiTexts.measureLabel(Language.TR, MeasureId.PFI));
    }

    @Test
    void everyWarningCodeHasATranslatedMessage() {
        for (Language l : Language.values()) {
            for (WarningCode c : WarningCode.values()) {
                assertText(UiTexts.warning(l, new Warning(c, Eye.OD, 1.5, "OneDrive")));
                assertText(UiTexts.warning(l, new Warning(c, null, Double.NaN)));
            }
        }
    }

    @Test
    void warningValuesAreFormattedPerCodeAndLanguage() {
        assertEquals("Eğik çekim: etiket eksen oranı 0,951 (< 0,97).",
                UiTexts.warning(Language.TR, new Warning(WarningCode.OBLIQUE_VIEW, null, 0.9512)));
        String canthus = UiTexts.warning(Language.EN, new Warning(WarningCode.CANTHUS_ORDER, Eye.OD, -1.234));
        assertTrue(canthus.startsWith("OD: ") && canthus.contains("(-1.23 mm)"), canthus);
        assertTrue(UiTexts.warning(Language.TR, new Warning(WarningCode.LOW_RESOLUTION, null, 140.4)).contains("140 px"));
        assertTrue(UiTexts.warning(Language.TR, new Warning(WarningCode.CLOUD_FOLDER_INPUT, null, Double.NaN,
                "OneDrive")).contains("(OneDrive)"));
    }

    @Test
    void enumLabels() {
        assertEquals("Sağ göz (OD)", UiTexts.eye(Language.TR, Eye.OD));
        assertEquals("Left eye (OS)", UiTexts.eye(Language.EN, Eye.OS));
        assertEquals("Post-op", UiTexts.timing(Language.TR, Timing.POST_OP));
        assertEquals("pupiller", UiTexts.axisSource(Language.TR, ReferenceAxes.Source.PUPILS));
        assertEquals("drawn line", UiTexts.axisSource(Language.EN, ReferenceAxes.Source.USER_LINE));
    }
}
