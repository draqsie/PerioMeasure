package com.periomeasure.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * Minimal, defensive EXIF reader for JPEG files: the orientation tag
 * (so phone photos are shown upright) and the capture date (to pre-fill
 * the photo date). Never throws: unreadable data gives the defaults
 * (orientation 1, no date).
 */
public final class Exif {

    /** Orientation (1-8, EXIF tag 0x0112) and capture date (null if unknown). */
    public static final class Info {
        public final int orientation;
        public final LocalDate dateTaken;

        Info(int orientation, LocalDate dateTaken) {
            this.orientation = orientation;
            this.dateTaken = dateTaken;
        }
    }

    private static final int MAX_HEADER_BYTES = 512 * 1024;
    private static final Info DEFAULT = new Info(1, null);

    private Exif() {
    }

    public static Info read(File f) {
        if (f == null || !f.isFile()) {
            return DEFAULT;
        }
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) Math.min(f.length(), MAX_HEADER_BYTES)];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) {
                    break;
                }
                n += r;
            }
            return parseJpeg(buf, n);
        } catch (IOException | RuntimeException e) {
            return DEFAULT;
        }
    }

    private static Info parseJpeg(byte[] b, int n) {
        if (n < 4 || u8(b, 0) != 0xFF || u8(b, 1) != 0xD8) {
            return DEFAULT;
        }
        int p = 2;
        while (p + 4 <= n) {
            if (u8(b, p) != 0xFF) {
                return DEFAULT;
            }
            int marker = u8(b, p + 1);
            if (marker == 0xDA || marker == 0xD9) {
                return DEFAULT; // start of scan / end of image: no EXIF found
            }
            int len = (u8(b, p + 2) << 8) | u8(b, p + 3);
            if (len < 2 || p + 2 + len > n) {
                return DEFAULT;
            }
            if (marker == 0xE1 && len >= 8 && b[p + 4] == 'E' && b[p + 5] == 'x' && b[p + 6] == 'i'
                    && b[p + 7] == 'f' && b[p + 8] == 0 && b[p + 9] == 0) {
                return parseTiff(b, p + 10, p + 2 + len);
            }
            p += 2 + len;
        }
        return DEFAULT;
    }

    private static Info parseTiff(byte[] b, int base, int end) {
        boolean le;
        if (b[base] == 'I' && b[base + 1] == 'I') {
            le = true;
        } else if (b[base] == 'M' && b[base + 1] == 'M') {
            le = false;
        } else {
            return DEFAULT;
        }
        if (u16(b, base + 2, le) != 42) {
            return DEFAULT;
        }
        int ifd0 = base + (int) u32(b, base + 4, le);
        int orientation = 1;
        LocalDate date = null;
        long exifPtr = -1;
        int count = u16(b, ifd0, le);
        for (int i = 0; i < count; i++) {
            int e = ifd0 + 2 + 12 * i;
            if (e + 12 > end) {
                break;
            }
            int tag = u16(b, e, le);
            if (tag == 0x0112) {
                int o = u16(b, e + 8, le);
                orientation = o >= 1 && o <= 8 ? o : 1;
            } else if (tag == 0x8769) {
                exifPtr = u32(b, e + 8, le);
            } else if (tag == 0x0132 && date == null) {
                date = dateAt(b, base + (int) u32(b, e + 8, le), end);
            }
        }
        if (exifPtr > 0) {
            int ifd = base + (int) exifPtr;
            if (ifd + 2 <= end) {
                int c = u16(b, ifd, le);
                for (int i = 0; i < c; i++) {
                    int e = ifd + 2 + 12 * i;
                    if (e + 12 > end) {
                        break;
                    }
                    if (u16(b, e, le) == 0x9003) {
                        LocalDate d = dateAt(b, base + (int) u32(b, e + 8, le), end);
                        if (d != null) {
                            date = d;
                        }
                    }
                }
            }
        }
        return new Info(orientation, date);
    }

    /** Parses "YYYY:MM:DD hh:mm:ss"; null if malformed or impossible. */
    private static LocalDate dateAt(byte[] b, int off, int end) {
        if (off < 0 || off + 10 > end) {
            return null;
        }
        String s = new String(b, off, 10, StandardCharsets.US_ASCII);
        if (!s.matches("\\d{4}:\\d{2}:\\d{2}")) {
            return null;
        }
        try {
            return LocalDate.of(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)),
                    Integer.parseInt(s.substring(8, 10)));
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static int u8(byte[] b, int i) {
        return b[i] & 0xFF;
    }

    private static int u16(byte[] b, int i, boolean le) {
        return le ? (u8(b, i) | (u8(b, i + 1) << 8)) : ((u8(b, i) << 8) | u8(b, i + 1));
    }

    private static long u32(byte[] b, int i, boolean le) {
        return le ? (u16(b, i, true) | ((long) u16(b, i + 2, true) << 16))
                : (((long) u16(b, i, false) << 16) | u16(b, i + 2, false));
    }

    /**
     * Returns a new image as the camera intended it to be viewed (lossless
     * 90-degree rotations / mirrors). The original file is never modified.
     */
    public static BufferedImage applyOrientation(BufferedImage img, int orientation) {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean swap = orientation >= 5 && orientation <= 8;
        int dw = swap ? h : w;
        int dh = swap ? w : h;
        BufferedImage out = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                int sx;
                int sy;
                switch (orientation) {
                    case 2:
                        sx = w - 1 - x;
                        sy = y;
                        break;
                    case 3:
                        sx = w - 1 - x;
                        sy = h - 1 - y;
                        break;
                    case 4:
                        sx = x;
                        sy = h - 1 - y;
                        break;
                    case 5:
                        sx = y;
                        sy = x;
                        break;
                    case 6:
                        sx = y;
                        sy = h - 1 - x;
                        break;
                    case 7:
                        sx = w - 1 - y;
                        sy = h - 1 - x;
                        break;
                    case 8:
                        sx = w - 1 - y;
                        sy = x;
                        break;
                    default:
                        sx = x;
                        sy = y;
                }
                out.setRGB(x, y, img.getRGB(sx, sy));
            }
        }
        return out;
    }
}
