package com.periomeasure.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.List;

import com.periomeasure.core.EllipseFitResult;
import com.periomeasure.core.Eye;
import com.periomeasure.core.ReferenceAxes;
import com.periomeasure.core.Vec2;
import com.periomeasure.session.Handle;
import com.periomeasure.session.Landmark;
import com.periomeasure.session.MeasurementSession;
import com.periomeasure.session.Step;

/**
 * Draws the session on top of the photograph (never into it): points as
 * crosshairs whose centre pixel is the point's own screen pixel, the live
 * sticker ellipse, the reference line, contours, and a dashed guide through
 * the pupil along the measurement vertical while lid points are marked.
 * Used by both the main viewer and the loupe.
 */
final class OverlayPainter {

    static final Color SELECTED = new Color(255, 255, 0);
    static final Color CALIBRATION = new Color(0, 255, 255);
    static final Color OD_COLOR = new Color(0, 230, 0);
    static final Color OS_COLOR = new Color(255, 0, 255);
    static final Color GUIDE = new Color(255, 140, 0);

    private static final int GAP = 3;
    private static final int ARM = 9;
    private static final int ARM_SELECTED = 14;

    private OverlayPainter() {
    }

    static void paint(Graphics2D g, MeasurementSession s, ViewState v, Handle selected, boolean finished,
            int imageW, int imageH) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setStroke(new BasicStroke(1f));
            EllipseFitResult fit = s.liveFit();
            if (fit != null) {
                Path2D.Double path = new Path2D.Double();
                for (int k = 0; k <= 256; k++) {
                    Vec2 q = fit.ellipse.pointAt(2 * Math.PI * k / 256);
                    if (k == 0) {
                        path.moveTo(v.toScreenX(q.x), v.toScreenY(q.y));
                    } else {
                        path.lineTo(v.toScreenX(q.x), v.toScreenY(q.y));
                    }
                }
                g2.setColor(CALIBRATION);
                g2.draw(path);
            }
            if (!finished) {
                drawGuide(g2, s, v, imageW + imageH);
            }
            List<Step> steps = s.steps();
            for (int i = 0; i < steps.size(); i++) {
                Step st = steps.get(i);
                List<Vec2> pts = s.points(i);
                Color col = colorFor(st);
                if (st.landmark == Landmark.CONTOUR || st.landmark == Landmark.REFERENCE_LINE) {
                    polyline(g2, pts, st.landmark == Landmark.CONTOUR, col, v);
                }
                for (int k = 0; k < pts.size(); k++) {
                    boolean sel = selected != null && selected.stepIndex == i && selected.pointIndex == k;
                    crosshair(g2, pts.get(k), sel ? SELECTED : col, sel ? ARM_SELECTED : ARM, v);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    static Color colorFor(Step st) {
        if (st.landmark == Landmark.CALIBRATION_EDGE) {
            return CALIBRATION;
        }
        if (st.eye == null) {
            return GUIDE;
        }
        return st.eye == Eye.OD ? OD_COLOR : OS_COLOR;
    }

    static boolean isVertical(Landmark l) {
        switch (l) {
            case UPPER_LID:
            case LOWER_LID:
            case UPPER_LID_CREASE:
            case BROW_LOWER_BORDER:
            case LIMBUS_UPPER:
            case LIMBUS_LOWER:
                return true;
            default:
                return false;
        }
    }

    private static void drawGuide(Graphics2D g2, MeasurementSession s, ViewState v, double len) {
        Step st = s.current();
        if (st.eye == null || !isVertical(st.landmark)) {
            return;
        }
        ReferenceAxes ax = s.previewAxes();
        Vec2 pupil = s.pupil(st.eye);
        if (ax == null || pupil == null) {
            return;
        }
        Vec2 a = pupil.minus(ax.v.times(len));
        Vec2 b = pupil.plus(ax.v.times(len));
        g2.setColor(GUIDE);
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f}, 0f));
        g2.draw(new Line2D.Double(v.toScreenX(a.x), v.toScreenY(a.y), v.toScreenX(b.x), v.toScreenY(b.y)));
        g2.setStroke(new BasicStroke(1f));
    }

    private static void polyline(Graphics2D g2, List<Vec2> pts, boolean close, Color col, ViewState v) {
        if (pts.size() < 2) {
            return;
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(v.toScreenX(pts.get(0).x), v.toScreenY(pts.get(0).y));
        for (int i = 1; i < pts.size(); i++) {
            path.lineTo(v.toScreenX(pts.get(i).x), v.toScreenY(pts.get(i).y));
        }
        if (close && pts.size() >= 3) {
            path.closePath();
        }
        g2.setColor(col);
        g2.draw(path);
    }

    private static void crosshair(Graphics2D g2, Vec2 p, Color col, int arm, ViewState v) {
        int[] c = v.toScreenPixel(p);
        g2.setColor(col);
        g2.fillRect(c[0], c[1], 1, 1);
        g2.drawLine(c[0] - arm, c[1], c[0] - GAP, c[1]);
        g2.drawLine(c[0] + GAP, c[1], c[0] + arm, c[1]);
        g2.drawLine(c[0], c[1] - arm, c[0], c[1] - GAP);
        g2.drawLine(c[0], c[1] + GAP, c[0], c[1] + arm);
    }
}
