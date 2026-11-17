package com.periomeasure;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Plugin version. Single source of truth is {@code pom.xml}; Maven resource
 * filtering writes it into {@code periomeasure-version.properties}.
 * The version is written into every CSV row so results stay traceable.
 */
public final class Version {

    static final String RESOURCE = "periomeasure-version.properties";

    private static final String VERSION = load();

    private Version() {
    }

    /** @return the plugin version, e.g. {@code 0.1.0} */
    public static String get() {
        return VERSION;
    }

    private static String load() {
        try (InputStream in = Version.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE + " (broken build)");
            }
            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            String v = p.getProperty("version");
            if (v == null || v.trim().isEmpty() || v.contains("${")) {
                throw new IllegalStateException("Version not filtered in " + RESOURCE + ": " + v);
            }
            return v.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
