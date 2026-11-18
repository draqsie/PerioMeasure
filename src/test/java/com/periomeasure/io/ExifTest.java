package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class ExifTest {

    /** Builds a TIFF/EXIF block with IFD0 {Orientation, ExifIFD pointer} and ExifIFD {DateTimeOriginal}. */
    static byte[] tiff(ByteOrder order, int orientation, String dateTime) {
        ByteBuffer b = ByteBuffer.allocate(200).order(order);
        b.put(order == ByteOrder.LITTLE_ENDIAN ? new byte[] {'I', 'I'} : new byte[] {'M', 'M'});
        b.putShort((short) 42);
        b.putInt(8); // IFD0 at offset 8
        int ifd0Entries = dateTime == null ? 1 : 2;
        b.putShort((short) ifd0Entries);
        b.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
        int exifIfd = 8 + 2 + 12 * ifd0Entries + 4;
        if (dateTime != null) {
            b.putShort((short) 0x8769).putShort((short) 4).putInt(1).putInt(exifIfd);
        }
        b.putInt(0); // no next IFD
        if (dateTime != null) {
            int strOffset = exifIfd + 2 + 12 + 4;
            b.putShort((short) 1);
            b.putShort((short) 0x9003).putShort((short) 2).putInt(20).putInt(strOffset);
            b.putInt(0);
            b.put((dateTime + "\0").getBytes(StandardCharsets.US_ASCII));
        }
        byte[] out = new byte[b.position()];
        b.flip();
        b.get(out);
        return out;
    }

    /** A real JPEG with an APP1/Exif segment inserted right after SOI. */
    static File jpegWithExif(String name, byte[] tiff) throws Exception {
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB), "jpg", jpeg);
        byte[] j = jpeg.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(j, 0, 2); // SOI
        int len = 2 + 6 + tiff.length;
        out.write(new byte[] {(byte) 0xFF, (byte) 0xE1, (byte) (len >> 8), (byte) len});
        out.write(new byte[] {'E', 'x', 'i', 'f', 0, 0});
        out.write(tiff);
        out.write(j, 2, j.length - 2);
        File dir = new File("target/test-output/exif");
        dir.mkdirs();
        File f = new File(dir, name);
        Files.write(f.toPath(), out.toByteArray());
        return f;
    }

    @Test
    void readsOrientationAndDateInBothByteOrders() throws Exception {
        for (ByteOrder o : new ByteOrder[] {ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN}) {
            Exif.Info info = Exif.read(jpegWithExif("o6_" + o + ".jpg", tiff(o, 6, "2026:03:02 10:11:12")));
            assertEquals(6, info.orientation, o.toString());
            assertEquals(LocalDate.of(2026, 3, 2), info.dateTaken, o.toString());
        }
    }

    @Test
    void missingDateGivesNull() throws Exception {
        Exif.Info info = Exif.read(jpegWithExif("o3.jpg", tiff(ByteOrder.BIG_ENDIAN, 3, null)));
        assertEquals(3, info.orientation);
        assertNull(info.dateTaken);
    }

    @Test
    void filesWithoutExifGiveDefaults() throws Exception {
        File dir = new File("target/test-output/exif");
        dir.mkdirs();
        File plain = new File(dir, "plain.jpg");
        ImageIO.write(new BufferedImage(8, 4, BufferedImage.TYPE_INT_RGB), "jpg", plain);
        assertEquals(1, Exif.read(plain).orientation);
        assertNull(Exif.read(plain).dateTaken);
        File junk = new File(dir, "junk.jpg");
        Files.write(junk.toPath(), new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 0, 2});
        assertEquals(1, Exif.read(junk).orientation);
        assertEquals(1, Exif.read(new File(dir, "missing.jpg")).orientation);
        assertEquals(1, Exif.read(null).orientation);
    }

    @Test
    void invalidDateOrOrientationIsIgnored() throws Exception {
        Exif.Info info = Exif.read(jpegWithExif("bad.jpg", tiff(ByteOrder.LITTLE_ENDIAN, 42, "0000:00:00 00:00:00")));
        assertEquals(1, info.orientation);
        assertNull(info.dateTaken);
    }

    // ------------------------------------------------------------------ orientation

    /** 3 x 2 test image with a distinct value per pixel: value = 10 * y + x. */
    private static BufferedImage sample() {
        BufferedImage img = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 3; x++) {
                img.setRGB(x, y, 10 * y + x);
            }
        }
        return img;
    }

    private static int[][] grid(BufferedImage img) {
        int[][] g = new int[img.getHeight()][img.getWidth()];
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                g[y][x] = img.getRGB(x, y) & 0xFFFFFF;
            }
        }
        return g;
    }

    @Test
    void orientationTransformsMatchTheExifSpecification() {
        // source:  0  1  2
        //         10 11 12
        assertEquals(java.util.Arrays.deepToString(new int[][] {{0, 1, 2}, {10, 11, 12}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 1))));
        assertEquals(java.util.Arrays.deepToString(new int[][] {{2, 1, 0}, {12, 11, 10}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 2))));
        assertEquals(java.util.Arrays.deepToString(new int[][] {{12, 11, 10}, {2, 1, 0}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 3))));
        assertEquals(java.util.Arrays.deepToString(new int[][] {{10, 11, 12}, {0, 1, 2}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 4))));
        // 6 = rotate 90 degrees clockwise to display correctly
        assertEquals(java.util.Arrays.deepToString(new int[][] {{10, 0}, {11, 1}, {12, 2}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 6))));
        // 8 = rotate 90 degrees counter-clockwise
        assertEquals(java.util.Arrays.deepToString(new int[][] {{2, 12}, {1, 11}, {0, 10}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 8))));
        // 5 = transpose, 7 = transverse
        assertEquals(java.util.Arrays.deepToString(new int[][] {{0, 10}, {1, 11}, {2, 12}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 5))));
        assertEquals(java.util.Arrays.deepToString(new int[][] {{12, 2}, {11, 1}, {10, 0}}),
                java.util.Arrays.deepToString(grid(Exif.applyOrientation(sample(), 7))));
    }
}
