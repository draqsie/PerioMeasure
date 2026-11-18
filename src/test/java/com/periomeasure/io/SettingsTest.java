package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.periomeasure.i18n.Language;

class SettingsTest {

    static final class MapStore implements PrefsStore {
        final Map<String, String> map = new HashMap<>();

        @Override
        public String get(String key, String def) {
            return map.containsKey(key) ? map.get(key) : def;
        }

        @Override
        public void set(String key, String value) {
            map.put(key, value);
        }
    }

    @Test
    void defaults() {
        Settings s = Settings.load(new MapStore());
        assertEquals(Language.TR, s.language);
        assertEquals(24.0, s.referenceDiameterMm, 0);
        assertEquals('.', s.decimalSeparator);
        assertEquals("", s.outputDir);
        assertEquals(6, s.zoomFactor);
    }

    @Test
    void roundTripThroughTheStore() {
        MapStore store = new MapStore();
        Settings s = Settings.defaults().withLanguage(Language.EN).withReferenceDiameterMm(20.5)
                .withDecimalSeparator(',').withOutputDir("D:\\research\\out").withZoomFactor(8);
        s.save(store);
        Settings back = Settings.load(store);
        assertEquals(Language.EN, back.language);
        assertEquals(20.5, back.referenceDiameterMm, 0);
        assertEquals(',', back.decimalSeparator);
        assertEquals("D:\\research\\out", back.outputDir);
        assertEquals(8, back.zoomFactor);
    }

    @Test
    void diameterIsStoredLocaleIndependently() {
        MapStore store = new MapStore();
        Settings.defaults().withReferenceDiameterMm(20.5).save(store);
        assertEquals("20.5", store.map.get(Settings.KEY_DIAMETER)); // tests run in tr_TR
    }

    @Test
    void corruptStoredValuesFallBackToDefaults() {
        MapStore store = new MapStore();
        store.set(Settings.KEY_LANGUAGE, "xx");
        store.set(Settings.KEY_DIAMETER, "abc");
        store.set(Settings.KEY_DECIMAL, "x");
        store.set(Settings.KEY_ZOOM, "12");
        Settings s = Settings.load(store);
        assertEquals(Language.TR, s.language);
        assertEquals(24.0, s.referenceDiameterMm, 0);
        assertEquals('.', s.decimalSeparator);
        assertEquals(6, s.zoomFactor);
        store.set(Settings.KEY_DIAMETER, "-3");
        assertEquals(24.0, Settings.load(store).referenceDiameterMm, 0);
    }

    @Test
    void invalidValuesAreRejectedBySetters() {
        Settings d = Settings.defaults();
        assertThrows(IllegalArgumentException.class, () -> d.withReferenceDiameterMm(0.5));
        assertThrows(IllegalArgumentException.class, () -> d.withReferenceDiameterMm(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> d.withReferenceDiameterMm(101));
        assertThrows(IllegalArgumentException.class, () -> d.withZoomFactor(3));
        assertThrows(IllegalArgumentException.class, () -> d.withZoomFactor(9));
        assertThrows(IllegalArgumentException.class, () -> d.withDecimalSeparator(';'));
    }

    @Test
    void outputDirectoryDefaultsToAnOutputFolderNextToThePhoto() {
        File photo = new File("C:\\dev\\PerioMeasure\\samples\\P017_pre.jpg");
        assertEquals(new File("C:\\dev\\PerioMeasure\\samples\\output"), Settings.defaults().outputDirFor(photo));
        assertEquals(new File("D:\\out"), Settings.defaults().withOutputDir("D:\\out").outputDirFor(photo));
        assertEquals(new File(System.getProperty("user.home"), "PerioMeasure" + File.separator + "output"),
                Settings.defaults().outputDirFor(null));
    }
}
