package com.periomeasure.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Draws the photograph with the view transform. When zoomed in (scale >= 1)
 * nearest-neighbour sampling at pixel centres shows sharp pixel blocks that
 * match the click mapping exactly; when zoomed out a reduced copy is drawn
 * smoothly.
 */
final class ImagePainter {

    private ImagePainter() {
    }

    static void draw(Graphics2D g, ImagePyramid p, ViewState v) {
        BufferedImage img = p.levelFor(v.scale);
        double rx = img.getWidth() / (double) p.width();
        double ry = img.getHeight() / (double) p.height();
        AffineTransform at = new AffineTransform();
        at.translate(-v.originX * v.scale, -v.originY * v.scale);
        at.scale(v.scale / rx, v.scale / ry);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, v.scale >= 1
                    ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                    : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, at, null);
        } finally {
            g2.dispose();
        }
    }
}
