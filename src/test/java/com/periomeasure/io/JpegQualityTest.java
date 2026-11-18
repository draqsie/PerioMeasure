package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.junit.jupiter.api.Test;

class JpegQualityTest {

    static File writeJpeg(float quality) throws IOException {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Random rnd = new Random(1);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                img.setRGB(x, y, rnd.nextInt(0xFFFFFF));
            }
        }
        File dir = new File("target/test-output/jpeg");
        dir.mkdirs();
        File f = new File(dir, "q" + Math.round(quality * 100) + ".jpg");
        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter w = it.next();
        ImageWriteParam p = w.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(quality);
        try (ImageOutputStream out = ImageIO.createImageOutputStream(f)) {
            w.setOutput(out);
            w.write(null, new IIOImage(img, null, null), p);
        } finally {
            w.dispose();
        }
        return f;
    }

    @Test
    void estimatesTheIjgQualityFromTheQuantisationTables() throws IOException {
        for (float q : new float[] {0.30f, 0.50f, 0.75f, 0.90f, 0.95f}) {
            assertEquals(q * 100, JpegQuality.estimate(writeJpeg(q)), 2, "quality " + q);
        }
    }

    @Test
    void nonJpegOrUnreadableFilesGiveMinusOne() throws IOException {
        File dir = new File("target/test-output/jpeg");
        dir.mkdirs();
        File png = new File(dir, "x.png");
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", png);
        assertEquals(-1, JpegQuality.estimate(png));
        File junk = new File(dir, "junk.jpg");
        Files.write(junk.toPath(), new byte[] {1, 2, 3});
        assertEquals(-1, JpegQuality.estimate(junk));
        assertEquals(-1, JpegQuality.estimate(new File(dir, "missing.jpg")));
    }
}
