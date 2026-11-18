package com.periomeasure.ui;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;
import com.periomeasure.io.Exif;
import com.periomeasure.io.ImageChecks;

import ij.ImagePlus;
import ij.io.Opener;

/**
 * Reads a photograph for measuring: ImageJ's opener (JPEG, PNG, TIFF incl.
 * 16-bit, BMP, GIF), then the EXIF orientation is applied so phone photos
 * are upright. The file itself is never modified.
 */
final class PhotoLoader {

    /** A loaded photo. */
    static final class Loaded {
        final File file;
        final BufferedImage image;
        final List<Warning> warnings;
        final LocalDate dateTaken;

        Loaded(File file, BufferedImage image, List<Warning> warnings, LocalDate dateTaken) {
            this.file = file;
            this.image = image;
            this.warnings = warnings;
            this.dateTaken = dateTaken;
        }
    }

    /** Why a file could not be loaded. */
    static final class LoadException extends Exception {
        private static final long serialVersionUID = 1L;

        final boolean heic;

        LoadException(String message, boolean heic) {
            super(message);
            this.heic = heic;
        }
    }

    private PhotoLoader() {
    }

    static boolean isHeic(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".heic") || n.endsWith(".heif");
    }

    static Loaded load(File f, Map<String, String> env) throws LoadException {
        if (isHeic(f)) {
            throw new LoadException(f.getName(), true);
        }
        Opener opener = new Opener();
        opener.setSilentMode(true);
        ImagePlus imp;
        try {
            imp = opener.openImage(f.getAbsolutePath());
        } catch (RuntimeException e) {
            imp = null;
        }
        if (imp == null || imp.getWidth() < 2 || imp.getHeight() < 2) {
            throw new LoadException(f.getAbsolutePath(), false);
        }
        boolean fileCalibrated = imp.getCalibration() != null && imp.getCalibration().scaled();
        BufferedImage rgb = toRgb(imp.getBufferedImage());
        imp.close();
        Exif.Info exif = Exif.read(f);
        List<Warning> warnings = new ArrayList<>();
        if (exif.orientation != 1) {
            rgb = Exif.applyOrientation(rgb, exif.orientation);
            warnings.add(new Warning(WarningCode.EXIF_ORIENTATION_APPLIED, null, exif.orientation));
        }
        warnings.addAll(ImageChecks.check(rgb.getWidth(), rgb.getHeight(), f, fileCalibrated, env));
        return new Loaded(f, rgb, warnings, exif.dateTaken);
    }

    /** Wraps an image that is already in memory (e.g. open in Fiji without a file). */
    static Loaded fromImage(BufferedImage img, Map<String, String> env) {
        BufferedImage rgb = toRgb(img);
        return new Loaded(null, rgb, ImageChecks.check(rgb.getWidth(), rgb.getHeight(), null, false, env), null);
    }

    /**
     * Wraps pixels pasted from the clipboard. They may be a reduced copy of the
     * photo and carry no file name, EXIF orientation or date, so the
     * {@link WarningCode#PASTED_IMAGE} warning is always added first.
     */
    static Loaded fromPasted(BufferedImage img, Map<String, String> env) {
        Loaded l = fromImage(img, env);
        List<Warning> warnings = new ArrayList<>();
        warnings.add(new Warning(WarningCode.PASTED_IMAGE, null, Double.NaN));
        warnings.addAll(l.warnings);
        return new Loaded(null, l.image, warnings, null);
    }

    static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}
