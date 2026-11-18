package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class AppIconTest {

    @Test
    void everyWindowsSizeIsRenderedSquareWithTransparentCorners() {
        List<BufferedImage> imgs = AppIcon.images();
        assertEquals(AppIcon.SIZES.length, imgs.size());
        for (int i = 0; i < imgs.size(); i++) {
            BufferedImage b = imgs.get(i);
            int s = AppIcon.SIZES[i];
            assertEquals(s, b.getWidth());
            assertEquals(s, b.getHeight());
            assertEquals(0, b.getRGB(0, 0) >>> 24, "corner must be transparent at " + s);
            assertEquals(255, b.getRGB(s / 2, s / 10) >>> 24, "tile must be opaque at " + s);
        }
    }

    @Test
    void theEyeIsInTheMiddleAndTheMeasureLineBelowIt() {
        BufferedImage b = AppIcon.render(256);
        int pupil = b.getRGB(128, 118) & 0xFFFFFF;
        assertTrue(brightness(pupil) < 60, Integer.toHexString(pupil));
        int white = b.getRGB(60, 118) & 0xFFFFFF;
        assertTrue(brightness(white) > 220, Integer.toHexString(white));
        int yellow = b.getRGB(128, 186);
        assertTrue(((yellow >> 16) & 0xFF) > 200 && ((yellow >> 8) & 0xFF) > 150 && (yellow & 0xFF) < 120,
                Integer.toHexString(yellow));
    }

    @Test
    void homeScreenIconIsFullyOpaqueBecauseIosRoundsTheCornersItself() throws Exception {
        BufferedImage b = AppIcon.renderSquare(180);
        for (int[] p : new int[][] {{0, 0}, {179, 0}, {0, 179}, {179, 179}, {90, 2}}) {
            assertEquals(255, b.getRGB(p[0], p[1]) >>> 24, p[0] + "," + p[1]);
        }
        File dir = new File("target/test-output/webicons");
        AppIcon.writeWebIcons(dir);
        assertEquals(180, ImageIO.read(new File(dir, "apple-touch-icon.png")).getWidth());
        assertEquals(512, ImageIO.read(new File(dir, "icon-512.png")).getWidth());
    }

    @Test
    void renderingIsDeterministic() {
        BufferedImage a = AppIcon.render(48);
        BufferedImage b = AppIcon.render(48);
        assertArrayEquals(a.getRGB(0, 0, 48, 48, null, 0, 48), b.getRGB(0, 0, 48, 48, null, 0, 48));
    }

    @Test
    void icoFileHasAValidDirectoryOfPngImages() throws Exception {
        File dir = new File("target/test-output/icon");
        dir.mkdirs();
        File ico = new File(dir, "PerioMeasure.ico");
        AppIcon.writeIco(ico);
        byte[] data = Files.readAllBytes(ico.toPath());
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, bb.getShort(0));
        assertEquals(1, bb.getShort(2)); // icon
        int n = bb.getShort(4);
        assertEquals(AppIcon.SIZES.length, n);
        for (int i = 0; i < n; i++) {
            int e = 6 + 16 * i;
            int w = data[e] & 0xFF;
            int size = bb.getInt(e + 8);
            int offset = bb.getInt(e + 12);
            assertEquals(AppIcon.SIZES[i] == 256 ? 0 : AppIcon.SIZES[i], w);
            assertEquals(32, bb.getShort(e + 6));
            assertTrue(offset + size <= data.length);
            BufferedImage png = ImageIO.read(new ByteArrayInputStream(data, offset, size));
            assertEquals(AppIcon.SIZES[i], png.getWidth());
        }
    }

    private static int brightness(int rgb) {
        return (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
    }
}
