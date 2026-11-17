package com.periomeasure.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Ellipse;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.PixelConvention;
import com.periomeasure.core.Vec2;

class SyntheticRendererTest {

    private static final SyntheticScene SCENE = SyntheticScene.of(FaceModel.typical(), 0.1, 8, false, 0.93);

    /**
     * The sticker is rasterised with exact area coverage under the project's
     * pixel convention, so its intensity-weighted centroid in the image must
     * coincide with the true ellipse centre. Using pixel corners instead of
     * centres would shift it by 0.5 px.
     */
    @Test
    void stickerCentroidAndAreaInTheImageMatchTheGeometry() {
        BufferedImage img = SyntheticRenderer.render(SCENE);
        Ellipse e = SCENE.stickerPx();
        int r = (int) Math.ceil(e.a) + 3;
        int skin = SyntheticRenderer.SKIN.getRed();
        int sticker = SyntheticRenderer.STICKER.getRed();
        double sum = 0;
        double sx = 0;
        double sy = 0;
        for (int j = (int) e.cy - r; j <= (int) e.cy + r; j++) {
            for (int i = (int) e.cx - r; i <= (int) e.cx + r; i++) {
                int red = (img.getRGB(i, j) >> 16) & 0xFF;
                double cov = (skin - red) / (double) (skin - sticker);
                Vec2 c = PixelConvention.centerOf(i, j);
                sum += cov;
                sx += cov * c.x;
                sy += cov * c.y;
            }
        }
        assertEquals(e.area(), sum, 1e-3 * e.area(), "area");
        assertEquals(e.cx, sx / sum, 0.02, "centroid x");
        assertEquals(e.cy, sy / sum, 0.02, "centroid y");
    }

    @Test
    void everyLandmarkAndTheStickerAreInsideTheImage() {
        BufferedImage img = SyntheticRenderer.render(SCENE);
        assertEquals(SCENE.width, img.getWidth());
        assertEquals(SCENE.height, img.getHeight());
        for (Eye eye : Eye.values()) {
            EyeLandmarks lm = SCENE.landmarksPx(eye);
            for (Vec2 p : new Vec2[] {lm.pupil, lm.upperLid, lm.lowerLid, lm.medialCanthus, lm.lateralCanthus}) {
                assertTrue(p.x > 0 && p.y > 0 && p.x < img.getWidth() && p.y < img.getHeight(), eye + " " + p);
            }
        }
        Ellipse e = SCENE.stickerPx();
        assertTrue(e.cx - e.a > 0 && e.cx + e.a < img.getWidth() && e.cy - e.a > 0 && e.cy + e.a < img.getHeight());
    }

    @Test
    void writerProducesImageAndGroundTruthCsv() throws Exception {
        File dir = new File("target/test-output/synthetic");
        List<File> files = SyntheticImageWriter.write(SCENE, dir, "unit_test_scene");
        assertEquals(2, files.size());
        BufferedImage back = ImageIO.read(files.get(0));
        assertEquals(SCENE.width, back.getWidth());
        List<String> csv = Files.readAllLines(files.get(1).toPath(), StandardCharsets.UTF_8);
        assertTrue(csv.get(0).startsWith("item,eye,x_px,y_px,value,unit"), csv.get(0));
        assertTrue(csv.stream().anyMatch(l -> l.startsWith("PFI,OD,")));
        // Decimal separator is always a dot, whatever the default locale (tests run in tr_TR).
        String scaleLine = csv.stream().filter(l -> l.startsWith("mm_per_px,,")).findFirst().orElse("");
        assertTrue(scaleLine.contains(",0.1"), scaleLine);
    }
}
