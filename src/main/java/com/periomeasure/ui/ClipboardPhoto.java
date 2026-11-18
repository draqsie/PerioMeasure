package com.periomeasure.ui;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads a photo from the clipboard or from a drag-and-drop, preferring the
 * original file over pixels:
 * <ol>
 * <li>a copied photo file (Explorer "Copy") — the original file is opened,
 * with full resolution, EXIF orientation/date and its file name;</li>
 * <li>copied text naming one photo file ("Copy as path", a file: URI);</li>
 * <li>copied pixels ("Copy image" in a viewer or browser) — kept exactly,
 * but they may be a reduced copy, so the caller must warn.</li>
 * </ol>
 * If several photos were copied nothing is guessed: {@link Content#severalPhotos}.
 * Swing-free apart from AWT data types, so it is unit-tested headless.
 */
final class ClipboardPhoto {

    private static final String[] PHOTO_EXTENSIONS = {".jpg", ".jpeg", ".png", ".tif", ".tiff", ".bmp", ".gif",
        ".heic", ".heif"};
    /** Longer clipboard text cannot be a file path (e.g. a copied document). */
    private static final int MAX_PATH_TEXT = 8192;

    static final class Content {
        /** The original photo file, or null. */
        final File file;
        /** Copied pixels (TYPE_INT_RGB, exact copy), or null. */
        final BufferedImage image;
        /** More than one photo was copied; neither file nor image is set. */
        final boolean severalPhotos;

        private Content(File file, BufferedImage image, boolean severalPhotos) {
            this.file = file;
            this.image = image;
            this.severalPhotos = severalPhotos;
        }
    }

    private ClipboardPhoto() {
    }

    /** @return true if the file name has a photo extension PerioMeasure opens (or explains, for HEIC) */
    static boolean isPhotoName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        for (String ext : PHOTO_EXTENSIONS) {
            if (n.endsWith(ext) && n.length() > ext.length()) {
                return true;
            }
        }
        return false;
    }

    /** @return what the transferable offers, or null if it holds no usable photo */
    static Content read(Transferable t) {
        if (t == null) {
            return null;
        }
        Content c = fromFiles(t);
        if (c == null) {
            c = fromText(t);
        }
        if (c == null) {
            c = fromImage(t);
        }
        return c;
    }

    private static Content fromFiles(Transferable t) {
        Object data = data(t, DataFlavor.javaFileListFlavor);
        if (!(data instanceof List)) {
            return null;
        }
        List<File> photos = new ArrayList<>();
        for (Object o : (List<?>) data) {
            if (o instanceof File && isPhotoFile((File) o)) {
                photos.add((File) o);
            }
        }
        return chosen(photos);
    }

    private static Content fromText(Transferable t) {
        Object data = data(t, DataFlavor.stringFlavor);
        if (!(data instanceof String) || ((String) data).length() > MAX_PATH_TEXT) {
            return null;
        }
        List<File> photos = new ArrayList<>();
        for (String line : ((String) data).split("\\r?\\n|\\r")) {
            File f = asFile(line);
            if (f != null && isPhotoFile(f)) {
                photos.add(f);
            }
        }
        return chosen(photos);
    }

    private static Content chosen(List<File> photos) {
        if (photos.isEmpty()) {
            return null;
        }
        if (photos.size() > 1) {
            return new Content(null, null, true);
        }
        return new Content(photos.get(0), null, false);
    }

    private static File asFile(String line) {
        String s = line.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.isEmpty()) {
            return null;
        }
        try {
            if (s.regionMatches(true, 0, "file:", 0, 5)) {
                return new File(new URI(s));
            }
            return new File(s);
        } catch (Exception e) {
            return null; // not a path
        }
    }

    private static boolean isPhotoFile(File f) {
        return f.isFile() && isPhotoName(f.getName());
    }

    private static Content fromImage(Transferable t) {
        Object data = data(t, DataFlavor.imageFlavor);
        if (!(data instanceof Image)) {
            return null;
        }
        BufferedImage copy = exactRgbCopy((Image) data);
        return copy == null ? null : new Content(null, copy, false);
    }

    /** Copies every pixel's RGB value unchanged (no scaling, no colour conversion). */
    private static BufferedImage exactRgbCopy(Image img) {
        PixelGrabber g = new PixelGrabber(img, 0, 0, -1, -1, true);
        try {
            if (!g.grabPixels(10_000)) {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        int w = g.getWidth();
        int h = g.getHeight();
        Object px = g.getPixels();
        if (w <= 0 || h <= 0 || !(px instanceof int[]) || ((int[]) px).length < w * h) {
            return null;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, w, h, (int[]) px, 0, w);
        return out;
    }

    /** The clipboard can be busy or the data stale; any failure just means "not this flavour". */
    private static Object data(Transferable t, DataFlavor flavor) {
        try {
            return t.isDataFlavorSupported(flavor) ? t.getTransferData(flavor) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
