package com.periomeasure.synthetic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.periomeasure.core.Ellipse;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.PixelConvention;
import com.periomeasure.core.Vec2;

/**
 * Renders a {@link SyntheticScene} to an RGB image. Facial features are
 * illustrative (anti-aliased Java2D). The calibration sticker is rasterised
 * with exact area coverage (8 x 8 supersampling on edge pixels) using
 * {@link PixelConvention}, because calibration accuracy depends on it.
 */
public final class SyntheticRenderer {

    public static final Color SKIN = new Color(224, 186, 160);
    public static final Color STICKER = new Color(30, 80, 200);
    private static final Color SCLERA = new Color(246, 244, 238);
    private static final Color IRIS = new Color(105, 72, 44);
    private static final Color PUPIL = new Color(18, 18, 18);
    private static final Color LID_LINE = new Color(95, 55, 45);
    private static final Color BROW = new Color(72, 52, 38);
    private static final int SUPERSAMPLE = 8;

    private SyntheticRenderer() {
    }

    public static BufferedImage render(SyntheticScene scene) {
        BufferedImage img = new BufferedImage(scene.width, scene.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(SKIN);
            g.fillRect(0, 0, scene.width, scene.height);
            double px = 1 / scene.mmPerPx; // pixels per mm
            for (Eye eye : Eye.values()) {
                drawEye(g, scene, eye, px);
            }
        } finally {
            g.dispose();
        }
        drawSticker(img, scene.stickerPx());
        return img;
    }

    private static void drawEye(Graphics2D g, SyntheticScene scene, Eye eye, double px) {
        EyeLandmarks mm = scene.face.eye(eye);
        EyeLandmarks lm = scene.landmarksPx(eye);

        // Brow: a band above the lower brow border.
        List<Vec2> brow = new ArrayList<>();
        for (int i = 0; i <= 16; i++) {
            double x = -16 + 2 * i;
            brow.add(scene.toImage(mm.browLowerBorder.plus(new Vec2(x, -0.004 * x * x))));
        }
        for (int i = 16; i >= 0; i--) {
            double x = -16 + 2 * i;
            brow.add(scene.toImage(mm.browLowerBorder.plus(new Vec2(x, 4.5 - 0.006 * x * x))));
        }
        g.setColor(BROW);
        g.fill(path(brow));

        // Upper lid crease: a thin arc through the crease point.
        List<Vec2> crease = new ArrayList<>();
        for (int i = 0; i <= 12; i++) {
            double x = -12 + 2 * i;
            crease.add(scene.toImage(mm.upperLidCrease.plus(new Vec2(x, -0.012 * x * x))));
        }
        g.setColor(LID_LINE);
        g.setStroke(new BasicStroke((float) (0.25 * px)));
        g.draw(openPath(crease));

        // Fissure, iris, pupil and corneal light reflex (clipped to the fissure).
        Path2D fissure = path(lm.contour);
        g.setColor(SCLERA);
        g.fill(fissure);
        g.setClip(fissure);
        fillCircle(g, lm.pupil, FaceModel.LIMBUS_RADIUS_MM * px, IRIS);
        fillCircle(g, lm.pupil, 1.8 * px, PUPIL);
        fillCircle(g, lm.pupil, 0.35 * px, Color.WHITE);
        g.setClip(null);
        g.setColor(LID_LINE);
        g.setStroke(new BasicStroke((float) (0.35 * px)));
        g.draw(fissure);
    }

    private static void fillCircle(Graphics2D g, Vec2 c, double r, Color color) {
        g.setColor(color);
        g.fill(new Ellipse2D.Double(c.x - r, c.y - r, 2 * r, 2 * r));
    }

    private static Path2D path(List<Vec2> pts) {
        Path2D p = openPath(pts);
        p.closePath();
        return p;
    }

    private static Path2D openPath(List<Vec2> pts) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(pts.get(0).x, pts.get(0).y);
        for (int i = 1; i < pts.size(); i++) {
            p.lineTo(pts.get(i).x, pts.get(i).y);
        }
        return p;
    }

    /** Exact-coverage rasterisation of the sticker, blended over the existing pixels. */
    static void drawSticker(BufferedImage img, Ellipse e) {
        int r = (int) Math.ceil(e.a) + 2;
        int i0 = Math.max(0, (int) Math.floor(e.cx) - r);
        int i1 = Math.min(img.getWidth() - 1, (int) Math.ceil(e.cx) + r);
        int j0 = Math.max(0, (int) Math.floor(e.cy) - r);
        int j1 = Math.min(img.getHeight() - 1, (int) Math.ceil(e.cy) + r);
        // A pixel whose centre is farther than half its diagonal from the edge is entirely in or out.
        double band = 0.75;
        for (int j = j0; j <= j1; j++) {
            for (int i = i0; i <= i1; i++) {
                double sd = e.signedDistance(PixelConvention.centerOf(i, j));
                double cov;
                if (sd <= -band) {
                    cov = 1;
                } else if (sd >= band) {
                    continue;
                } else {
                    int inside = 0;
                    for (int sy = 0; sy < SUPERSAMPLE; sy++) {
                        for (int sx = 0; sx < SUPERSAMPLE; sx++) {
                            Vec2 q = new Vec2(i + (sx + 0.5) / SUPERSAMPLE, j + (sy + 0.5) / SUPERSAMPLE);
                            if (e.contains(q)) {
                                inside++;
                            }
                        }
                    }
                    cov = inside / (double) (SUPERSAMPLE * SUPERSAMPLE);
                }
                int old = img.getRGB(i, j);
                img.setRGB(i, j, blend(old, STICKER, cov));
            }
        }
    }

    private static int blend(int rgb, Color c, double cov) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = (int) Math.round(cov * c.getRed() + (1 - cov) * r);
        g = (int) Math.round(cov * c.getGreen() + (1 - cov) * g);
        b = (int) Math.round(cov * c.getBlue() + (1 - cov) * b);
        return (r << 16) | (g << 8) | b;
    }
}
