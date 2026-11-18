package com.periomeasure.session;

import com.periomeasure.core.PixelConvention;
import com.periomeasure.core.Vec2;

/** Geometry of "zoom to magnification m around a point" (integer source rectangle). */
public final class ZoomPlan {

    private ZoomPlan() {
    }

    /**
     * @param center  image point to centre on
     * @param mag     integer magnification
     * @param canvasW canvas width (screen px)
     * @param canvasH canvas height (screen px)
     * @return source rectangle (image px) that fits the canvas, clamped inside the image
     */
    public static IntRect sourceRect(Vec2 center, int mag, int canvasW, int canvasH, int imageW, int imageH) {
        int w = Math.min(imageW, (int) Math.ceil(canvasW / (double) mag));
        int h = Math.min(imageH, (int) Math.ceil(canvasH / (double) mag));
        int x = clamp(PixelConvention.indexOf(center.x) - w / 2, 0, imageW - w);
        int y = clamp(PixelConvention.indexOf(center.y) - h / 2, 0, imageH - h);
        return new IntRect(x, y, w, h);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
