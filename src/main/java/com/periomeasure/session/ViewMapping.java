package com.periomeasure.session;

import com.periomeasure.core.Vec2;

/**
 * The single definition of the screen &lt;-&gt; image mapping for clicking and
 * drawing. With source-rectangle origin (srcX, srcY) and magnification m,
 * screen pixel sx shows image coordinates [srcX + sx/m, srcX + (sx+1)/m).
 *
 * <p>A click is mapped to the CENTRE of that interval (ImageJ's own
 * {@code offScreenXD} returns its left edge, a bias of 0.5/m pixels). A
 * point is drawn on the screen pixel that contains it, so a marker always
 * appears exactly on the clicked screen pixel.
 */
public final class ViewMapping {

    private ViewMapping() {
    }

    public static Vec2 screenToImage(int sx, int sy, double srcX, double srcY, double mag) {
        return new Vec2(srcX + (sx + 0.5) / mag, srcY + (sy + 0.5) / mag);
    }

    /** @return {screenX, screenY} of the screen pixel containing image point {@code p} */
    public static int[] imageToScreenPixel(Vec2 p, double srcX, double srcY, double mag) {
        return new int[] {(int) Math.floor((p.x - srcX) * mag), (int) Math.floor((p.y - srcY) * mag)};
    }

    /** @return distance between two image points, in screen pixels */
    public static double screenDistance(Vec2 a, Vec2 b, double mag) {
        return a.distanceTo(b) * mag;
    }
}
