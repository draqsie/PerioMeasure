package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.periomeasure.i18n.Language;

import ij.plugin.PlugIn;

/** Guards the Fiji menu registration so a broken entry can never ship. */
class PluginsConfigTest {

    private static final Pattern ENTRY = Pattern.compile("^(Plugins>PerioMeasure), \"([^\"]+)\", ([\\w.]+)$");

    @Test
    void pluginsConfigIsAsciiAndEveryEntryIsALoadablePlugIn() throws Exception {
        byte[] bytes = readResource("/plugins.config");
        for (byte b : bytes) {
            assertTrue((b & 0xFF) < 0x80, "plugins.config must be ASCII-only (ImageJ reads it with the platform charset)");
        }

        List<String> entries = new ArrayList<>();
        for (String line : new String(bytes, StandardCharsets.US_ASCII).split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
                entries.add(t);
            }
        }
        assertFalse(entries.isEmpty(), "plugins.config has no entries");

        Set<String> labels = new HashSet<>();
        for (String e : entries) {
            Matcher m = ENTRY.matcher(e);
            assertTrue(m.matches(), "malformed plugins.config line: " + e);
            assertTrue(labels.add(m.group(2)), "duplicate menu label: " + m.group(2));

            Class<?> c = Class.forName(m.group(3));
            assertTrue(PlugIn.class.isAssignableFrom(c), c + " must implement ij.plugin.PlugIn");
            assertTrue(Modifier.isPublic(c.getModifiers()), c + " must be public");
            assertNotNull(c.getConstructor(), c + " needs a public no-arg constructor");
        }
    }

    @Test
    void aboutTextsContainVersionAndDisclaimer() {
        assertTrue(AboutPlugin.body(Language.TR).contains(com.periomeasure.Version.get()));
        assertTrue(AboutPlugin.body(Language.TR).contains("tıbbi cihaz değildir"));
        assertTrue(AboutPlugin.body(Language.EN).contains("not a medical device"));
        assertEquals("About PerioMeasure", AboutPlugin.title(Language.EN));
    }

    private static byte[] readResource(String name) throws Exception {
        try (InputStream in = PluginsConfigTest.class.getResourceAsStream(name)) {
            assertNotNull(in, "missing resource " + name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }
}
