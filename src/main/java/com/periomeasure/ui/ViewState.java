package com.periomeasure.ui;

import com.periomeasure.core.Vec2;
import com.periomeasure.session.ViewMapping;

/**
 * Immutable view transform of the image viewer and the loupe:
 * {@code screen = (image - origin) * scale}. Clicks are mapped with
 * {@link ViewMapping} (pixel-centre convention), so what is painted and what
 * a click means always agree.
 */
public final class ViewState {

    public static final double MIN_SCALE = 0.02;
    public static final double MAX_SCALE = 32;

    /** Image coordinate shown at the left edge of the view. */
    public final double originX;
    /** Image coordinate shown at the top edge of the view. */
    public final double originY;
    /** Screen pixels per image pixel. */
    public final double scale;

    public ViewState(double originX, double originY, double scale) {
        this.originX = originX;
        this.originY = originY;
        this.scale = scale;
    }

    /** Whole image visible and centred. */
    public static ViewState fit(int imageW, int imageH, int viewW, int viewH) {
        double s = Math.min(viewW / (double) imageW, viewH / (double) imageH);
        s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
        return new ViewState(-(viewW - imageW * s) / (2 * s), -(viewH - imageH * s) / (2 * s), s);
    }

    /** Point {@code c} in the middle of a view of the given size. */
    public static ViewState centeredOn(Vec2 c, double scale, int viewW, int viewH) {
        return new ViewState(c.x - viewW / 2.0 / scale, c.y - viewH / 2.0 / scale, scale);
    }

    public Vec2 screenToImage(int sx, int sy) {
        return ViewMapping.screenToImage(sx, sy, originX, originY, scale);
    }

    public int[] toScreenPixel(Vec2 p) {
        return ViewMapping.imageToScreenPixel(p, originX, originY, scale);
    }

    public double toScreenX(double x) {
        return (x - originX) * scale;
    }

    public double toScreenY(double y) {
        return (y - originY) * scale;
    }

    /** Zooms by {@code factor} keeping the image point under screen pixel (sx, sy) in place. */
    public ViewState zoomAt(double factor, int sx, int sy) {
        double ns = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
        double cx = sx + 0.5;
        double cy = sy + 0.5;
        double ix = originX + cx / scale;
        double iy = originY + cy / scale;
        return new ViewState(ix - cx / ns, iy - cy / ns, ns);
    }

    /** Moves the image by (dx, dy) screen pixels (follows the mouse). */
    public ViewState pan(int dx, int dy) {
        return new ViewState(originX - dx / scale, originY - dy / scale, scale);
    }
}
