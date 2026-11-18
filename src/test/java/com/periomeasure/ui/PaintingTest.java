package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.EyeSelection;
import com.periomeasure.core.PixelConvention;
import com.periomeasure.core.Vec2;
import com.periomeasure.session.AxisMode;
import com.periomeasure.session.Handle;
import com.periomeasure.session.MeasurementSession;
import com.periomeasure.session.SessionTestAccess;

/**
 * What is painted must agree with what a click means: the image pixel shown
 * under a screen pixel is exactly the pixel that a click there selects, and
 * a marker is painted on the screen pixel that contains its point.
 */
class PaintingTest {

    private static BufferedImage randomImage(int w, int h, long seed) {
        Random rnd = new Random(seed);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, rnd.nextInt(0xFFFFFF));
            }
        }
        return img;
    }

    private static BufferedImage canvas(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    void displayedPixelIsThePixelAClickSelectsWhenZoomedIn() {
        BufferedImage img = randomImage(60, 40, 1);
        ImagePyramid pyramid = new ImagePyramid(img);
        Random rnd = new Random(2);
        for (double scale : new double[] {1, 1.6, 2, 3.7, 6, 8, 12.5}) {
            ViewState v = new ViewState(rnd.nextDouble() * 10 - 3, rnd.nextDouble() * 8 - 2, scale);
            BufferedImage out = canvas(200, 150);
            Graphics2D g = out.createGraphics();
            ImagePainter.draw(g, pyramid, v);
            g.dispose();
            for (int k = 0; k < 400; k++) {
                int sx = rnd.nextInt(200);
                int sy = rnd.nextInt(150);
                Vec2 p = v.screenToImage(sx, sy);
                int ix = PixelConvention.indexOf(p.x);
                int iy = PixelConvention.indexOf(p.y);
                if (ix < 0 || iy < 0 || ix >= 60 || iy >= 40) {
                    continue;
                }
                assertEquals(img.getRGB(ix, iy) & 0xFFFFFF, out.getRGB(sx, sy) & 0xFFFFFF,
                        "scale " + scale + " screen (" + sx + "," + sy + ")");
            }
        }
    }

    @Test
    void zoomedOutDisplayCoversTheSameAreaAsTheMapping() {
        BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D gi = img.createGraphics();
        gi.setColor(java.awt.Color.WHITE);
        gi.fillRect(0, 0, 1000, 800);
        gi.dispose();
        ViewState v = ViewState.fit(1000, 800, 300, 300);
        BufferedImage out = canvas(300, 300);
        Graphics2D g = out.createGraphics();
        ImagePainter.draw(g, new ImagePyramid(img), v);
        g.dispose();
        // image spans y from 30 to 270 on screen (scale 0.3); outside stays black
        assertEquals(0xFFFFFF, out.getRGB(150, 150) & 0xFFFFFF);
        assertEquals(0x000000, out.getRGB(150, 10) & 0xFFFFFF);
        assertEquals(0x000000, out.getRGB(150, 290) & 0xFFFFFF);
    }

    private static MeasurementSession sessionAtPupil() {
        MeasurementSession s = new MeasurementSession(
                SessionTestAccess.setup(EyeSelection.BOTH, AxisMode.IMAGE_HORIZONTAL), 24);
        for (int i = 0; i < 8; i++) {
            s.place(new Vec2(200 + 60 * Math.cos(i * 0.785), 150 + 60 * Math.sin(i * 0.785)));
        }
        s.next();
        return s;
    }

    @Test
    void selectedPointIsHighlightedOnItsScreenPixelAndOthersUseTheirStepColour() {
        MeasurementSession s = sessionAtPupil();
        s.place(new Vec2(123.4, 77.9));
        Handle selected = new Handle(s.currentIndex(), 0);
        ViewState v = new ViewState(100.3, 60.1, 6);
        BufferedImage out = canvas(300, 200);
        Graphics2D g = out.createGraphics();
        OverlayPainter.paint(g, s, v, selected, false, 1000, 1000);
        g.dispose();
        int[] p = v.toScreenPixel(new Vec2(123.4, 77.9));
        assertEquals(OverlayPainter.SELECTED.getRGB() & 0xFFFFFF, out.getRGB(p[0], p[1]) & 0xFFFFFF);

        Vec2 cal = s.points(0).get(0);
        ViewState wide = ViewState.fit(400, 300, 400, 300);
        BufferedImage out2 = canvas(400, 300);
        Graphics2D g2 = out2.createGraphics();
        OverlayPainter.paint(g2, s, wide, selected, false, 400, 300);
        g2.dispose();
        int[] c = wide.toScreenPixel(cal);
        assertEquals(OverlayPainter.CALIBRATION.getRGB() & 0xFFFFFF, out2.getRGB(c[0], c[1]) & 0xFFFFFF);
    }

    @Test
    void verticalGuideIsDrawnThroughThePupilWhileMarkingLids() {
        MeasurementSession s = sessionAtPupil();
        s.place(new Vec2(150, 150));
        s.next();
        s.place(new Vec2(350, 150));
        s.next(); // now: upper lid OD
        ViewState v = ViewState.fit(400, 300, 400, 300);
        BufferedImage out = canvas(400, 300);
        Graphics2D g = out.createGraphics();
        OverlayPainter.paint(g, s, v, null, false, 400, 300);
        g.dispose();
        int[] p = v.toScreenPixel(new Vec2(150, 150));
        boolean found = false;
        for (int dy = 20; dy < 60; dy++) {
            found |= (out.getRGB(p[0], p[1] - dy) & 0xFFFFFF) == (OverlayPainter.GUIDE.getRGB() & 0xFFFFFF);
        }
        assertTrue(found, "dashed guide above the pupil");
    }

    @Test
    void loupeShowsTheCentrePixelUnderItsCrosshair() {
        BufferedImage img = randomImage(80, 60, 5);
        ViewState loupe = ViewState.centeredOn(new Vec2(40.5, 30.5), 8, 161, 161);
        BufferedImage out = canvas(161, 161);
        Graphics2D g = out.createGraphics();
        ImagePainter.draw(g, new ImagePyramid(img), loupe);
        g.dispose();
        // the screen pixel left of the centre still shows image pixel (40, 30)
        assertEquals(img.getRGB(40, 30) & 0xFFFFFF, out.getRGB(79, 80) & 0xFFFFFF);
        // the centre screen pixel of an odd-sized loupe maps exactly to the centre point
        assertEquals(new Vec2(40.5, 30.5), loupe.screenToImage(80, 80));
    }
}
