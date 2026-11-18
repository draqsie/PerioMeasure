package com.periomeasure.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The photograph plus successively halved copies, so that a zoomed-out
 * view of a large photo (e.g. 24 MP) is drawn quickly and smoothly. Only
 * the display uses the reduced copies; measurements always use full-
 * resolution coordinates.
 */
public final class ImagePyramid {

    private static final int SMALLEST = 1024;

    private final List<BufferedImage> levels;

    public ImagePyramid(BufferedImage full) {
        List<BufferedImage> l = new ArrayList<>();
        l.add(full);
        BufferedImage cur = full;
        while (Math.max(cur.getWidth(), cur.getHeight()) > SMALLEST) {
            int w = Math.max(1, (cur.getWidth() + 1) / 2);
            int h = Math.max(1, (cur.getHeight() + 1) / 2);
            BufferedImage next = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cur, 0, 0, w, h, null);
            g.dispose();
            l.add(next);
            cur = next;
        }
        this.levels = Collections.unmodifiableList(l);
    }

    public BufferedImage full() {
        return levels.get(0);
    }

    public int width() {
        return levels.get(0).getWidth();
    }

    public int height() {
        return levels.get(0).getHeight();
    }

    /** @return the smallest level whose resolution is still at least {@code scale} of the original */
    BufferedImage levelFor(double scale) {
        BufferedImage best = levels.get(0);
        for (BufferedImage b : levels) {
            if (b.getWidth() / (double) width() >= scale) {
                best = b;
            }
        }
        return best;
    }
}
