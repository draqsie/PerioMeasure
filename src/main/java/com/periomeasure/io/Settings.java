package com.periomeasure.io;

import java.io.File;
import java.util.Objects;

import com.periomeasure.i18n.Language;

/**
 * Immutable user settings, persisted through a {@link PrefsStore}
 * (ImageJ preferences in Fiji). Corrupt stored values fall back to the
 * defaults; the {@code with...} methods reject invalid values.
 */
public final class Settings {

    public static final String KEY_LANGUAGE = "periomeasure.language";
    public static final String KEY_DIAMETER = "periomeasure.diameter";
    public static final String KEY_DECIMAL = "periomeasure.decimal";
    public static final String KEY_OUTPUT = "periomeasure.output";
    public static final String KEY_ZOOM = "periomeasure.zoom";

    public static final double MIN_DIAMETER_MM = 1;
    public static final double MAX_DIAMETER_MM = 100;
    public static final int MIN_ZOOM = 4;
    public static final int MAX_ZOOM = 8;
    public static final int DEFAULT_ZOOM = 6;

    public final Language language;
    /** True diameter of the calibration sticker (mm). */
    public final double referenceDiameterMm;
    /** CSV decimal separator: '.' (default) or ','. */
    public final char decimalSeparator;
    /** Output folder; empty = an "output" folder next to the photograph. */
    public final String outputDir;
    /** Magnification used for precise clicking (4-8). */
    public final int zoomFactor;

    private Settings(Language language, double referenceDiameterMm, char decimalSeparator, String outputDir,
            int zoomFactor) {
        this.language = Objects.requireNonNull(language, "language");
        if (!validDiameter(referenceDiameterMm)) {
            throw new IllegalArgumentException("Reference diameter must be between " + MIN_DIAMETER_MM + " and "
                    + MAX_DIAMETER_MM + " mm: " + referenceDiameterMm);
        }
        if (decimalSeparator != '.' && decimalSeparator != ',') {
            throw new IllegalArgumentException("Decimal separator must be '.' or ','");
        }
        if (zoomFactor < MIN_ZOOM || zoomFactor > MAX_ZOOM) {
            throw new IllegalArgumentException("Zoom must be between " + MIN_ZOOM + " and " + MAX_ZOOM);
        }
        this.referenceDiameterMm = referenceDiameterMm;
        this.decimalSeparator = decimalSeparator;
        this.outputDir = outputDir == null ? "" : outputDir.trim();
        this.zoomFactor = zoomFactor;
    }

    public static Settings defaults() {
        return new Settings(Language.DEFAULT, 24.0, '.', "", DEFAULT_ZOOM);
    }

    public static Settings load(PrefsStore store) {
        Settings d = defaults();
        Language lang = Language.fromCode(store.get(KEY_LANGUAGE, d.language.code()));
        double diameter = d.referenceDiameterMm;
        try {
            double v = Double.parseDouble(store.get(KEY_DIAMETER, "").trim());
            if (validDiameter(v)) {
                diameter = v;
            }
        } catch (NumberFormatException e) {
            // keep default
        }
        String dec = store.get(KEY_DECIMAL, ".");
        char decimal = ",".equals(dec) ? ',' : '.';
        String out = store.get(KEY_OUTPUT, "");
        int zoom = d.zoomFactor;
        try {
            int z = Integer.parseInt(store.get(KEY_ZOOM, "").trim());
            if (z >= MIN_ZOOM && z <= MAX_ZOOM) {
                zoom = z;
            }
        } catch (NumberFormatException e) {
            // keep default
        }
        return new Settings(lang, diameter, decimal, out, zoom);
    }

    public void save(PrefsStore store) {
        store.set(KEY_LANGUAGE, language.code());
        store.set(KEY_DIAMETER, Double.toString(referenceDiameterMm));
        store.set(KEY_DECIMAL, String.valueOf(decimalSeparator));
        store.set(KEY_OUTPUT, outputDir);
        store.set(KEY_ZOOM, Integer.toString(zoomFactor));
    }

    public Settings withLanguage(Language l) {
        return new Settings(l, referenceDiameterMm, decimalSeparator, outputDir, zoomFactor);
    }

    public Settings withReferenceDiameterMm(double d) {
        return new Settings(language, d, decimalSeparator, outputDir, zoomFactor);
    }

    public Settings withDecimalSeparator(char c) {
        return new Settings(language, referenceDiameterMm, c, outputDir, zoomFactor);
    }

    public Settings withOutputDir(String dir) {
        return new Settings(language, referenceDiameterMm, decimalSeparator, dir, zoomFactor);
    }

    public Settings withZoomFactor(int z) {
        return new Settings(language, referenceDiameterMm, decimalSeparator, outputDir, z);
    }

    /**
     * @param photo the photograph file, or null if the image was never saved
     * @return the folder results are written to
     */
    public File outputDirFor(File photo) {
        if (!outputDir.isEmpty()) {
            return new File(outputDir);
        }
        if (photo != null && photo.getAbsoluteFile().getParentFile() != null) {
            return new File(photo.getAbsoluteFile().getParentFile(), "output");
        }
        return new File(new File(System.getProperty("user.home"), "PerioMeasure"), "output");
    }

    private static boolean validDiameter(double d) {
        return !Double.isNaN(d) && d >= MIN_DIAMETER_MM && d <= MAX_DIAMETER_MM;
    }
}
