package com.periomeasure.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.UIManager;

import com.formdev.flatlaf.util.UIScale;

/**
 * Small vector icons drawn with Java2D on a 20 x 20 grid, scaled for the
 * screen and coloured from the current theme (no image files needed).
 */
final class Icons {

    enum Kind {
        OPEN, DEMO, FIT, ZOOM_IN, ZOOM_OUT, SETTINGS, HELP, SUN, MOON, GLOBE, UNDO, BACK, NEXT, SKIP, CHECK, COPY,
        FOLDER, REFRESH, INFO, PASTE
    }

    private Icons() {
    }

    static Icon of(Kind k) {
        return new VectorIcon(k, null);
    }

    /** Icon in a fixed colour (e.g. white on an accent button). */
    static Icon of(Kind k, Color c) {
        return new VectorIcon(k, c);
    }

    private static final class VectorIcon implements Icon {
        private final Kind kind;
        private final Color fixed;

        VectorIcon(Kind kind, Color fixed) {
            this.kind = kind;
            this.fixed = fixed;
        }

        @Override
        public int getIconWidth() {
            return UIScale.scale(20);
        }

        @Override
        public int getIconHeight() {
            return UIScale.scale(20);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double s = getIconWidth() / 20.0;
                g2.scale(s, s);
                Color col = fixed;
                if (col == null) {
                    col = c != null && !c.isEnabled() ? UIManager.getColor("Label.disabledForeground")
                            : UIManager.getColor("Label.foreground");
                }
                g2.setColor(col == null ? Color.DARK_GRAY : col);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                draw(g2, kind);
            } finally {
                g2.dispose();
            }
        }
    }

    private static void draw(Graphics2D g, Kind k) {
        switch (k) {
            case OPEN: {
                Path2D p = new Path2D.Double();
                p.moveTo(2, 5);
                p.lineTo(8, 5);
                p.lineTo(10, 7);
                p.lineTo(18, 7);
                p.lineTo(18, 16);
                p.lineTo(2, 16);
                p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(10, 13.5, 10, 9.5));
                g.draw(new Line2D.Double(8, 11.5, 10, 9.5));
                g.draw(new Line2D.Double(12, 11.5, 10, 9.5));
                break;
            }
            case FOLDER: {
                Path2D p = new Path2D.Double();
                p.moveTo(2, 5);
                p.lineTo(8, 5);
                p.lineTo(10, 7);
                p.lineTo(18, 7);
                p.lineTo(18, 16);
                p.lineTo(2, 16);
                p.closePath();
                g.draw(p);
                break;
            }
            case DEMO:
                g.draw(new Ellipse2D.Double(3, 3, 14, 14));
                g.fill(new Ellipse2D.Double(6.6, 7.2, 2, 2));
                g.fill(new Ellipse2D.Double(11.4, 7.2, 2, 2));
                g.draw(new Arc2D.Double(6.5, 8, 7, 6, 200, 140, Arc2D.OPEN));
                break;
            case FIT:
                g.draw(new Line2D.Double(3, 7, 3, 3));
                g.draw(new Line2D.Double(3, 3, 7, 3));
                g.draw(new Line2D.Double(13, 3, 17, 3));
                g.draw(new Line2D.Double(17, 3, 17, 7));
                g.draw(new Line2D.Double(17, 13, 17, 17));
                g.draw(new Line2D.Double(17, 17, 13, 17));
                g.draw(new Line2D.Double(7, 17, 3, 17));
                g.draw(new Line2D.Double(3, 17, 3, 13));
                g.draw(new RoundRectangle2D.Double(7, 7, 6, 6, 2, 2));
                break;
            case ZOOM_IN:
            case ZOOM_OUT:
                g.draw(new Ellipse2D.Double(3, 3, 11, 11));
                g.draw(new Line2D.Double(12.5, 12.5, 17, 17));
                g.draw(new Line2D.Double(6, 8.5, 11, 8.5));
                if (k == Kind.ZOOM_IN) {
                    g.draw(new Line2D.Double(8.5, 6, 8.5, 11));
                }
                break;
            case SETTINGS:
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.draw(new Line2D.Double(10 + 5.6 * Math.cos(a), 10 + 5.6 * Math.sin(a), 10 + 8 * Math.cos(a),
                            10 + 8 * Math.sin(a)));
                }
                g.draw(new Ellipse2D.Double(4.5, 4.5, 11, 11));
                g.draw(new Ellipse2D.Double(7.5, 7.5, 5, 5));
                break;
            case HELP:
                g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15));
                g.draw(new Arc2D.Double(7.3, 5.5, 5.4, 5, 160, -230, Arc2D.OPEN));
                g.draw(new Line2D.Double(10, 10.6, 10, 11.8));
                g.fill(new Ellipse2D.Double(9.1, 13.6, 1.8, 1.8));
                break;
            case INFO:
                g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15));
                g.draw(new Line2D.Double(10, 9, 10, 14));
                g.fill(new Ellipse2D.Double(9.1, 5.4, 1.8, 1.8));
                break;
            case SUN:
                g.draw(new Ellipse2D.Double(6.5, 6.5, 7, 7));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g.draw(new Line2D.Double(10 + 5.4 * Math.cos(a), 10 + 5.4 * Math.sin(a), 10 + 7.6 * Math.cos(a),
                            10 + 7.6 * Math.sin(a)));
                }
                break;
            case MOON: {
                java.awt.geom.Area a = new java.awt.geom.Area(new Ellipse2D.Double(3, 3, 14, 14));
                a.subtract(new java.awt.geom.Area(new Ellipse2D.Double(7.5, 1.5, 12, 12)));
                g.fill(a);
                break;
            }
            case GLOBE:
                g.draw(new Ellipse2D.Double(2.5, 2.5, 15, 15));
                g.draw(new Ellipse2D.Double(6.5, 2.5, 7, 15));
                g.draw(new Line2D.Double(2.5, 10, 17.5, 10));
                break;
            case UNDO: {
                g.draw(new Arc2D.Double(4, 5, 12, 11, 90, -200, Arc2D.OPEN));
                g.draw(new Line2D.Double(10, 5, 6.5, 2.5));
                g.draw(new Line2D.Double(10, 5, 6.5, 7.5));
                break;
            }
            case BACK:
                g.draw(new Line2D.Double(12, 4, 6, 10));
                g.draw(new Line2D.Double(6, 10, 12, 16));
                break;
            case NEXT:
                g.draw(new Line2D.Double(8, 4, 14, 10));
                g.draw(new Line2D.Double(14, 10, 8, 16));
                break;
            case SKIP:
                g.draw(new Line2D.Double(4, 4, 10, 10));
                g.draw(new Line2D.Double(10, 10, 4, 16));
                g.draw(new Line2D.Double(10, 4, 16, 10));
                g.draw(new Line2D.Double(16, 10, 10, 16));
                break;
            case CHECK:
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(4, 10.5, 8.5, 15));
                g.draw(new Line2D.Double(8.5, 15, 16.5, 5));
                break;
            case COPY:
                g.draw(new RoundRectangle2D.Double(7, 6, 10, 11, 2, 2));
                g.draw(new Line2D.Double(4, 13, 4, 4));
                g.draw(new Line2D.Double(4, 4, 12, 4));
                break;
            case PASTE: // clipboard board with its clip
                g.draw(new RoundRectangle2D.Double(4, 4, 12, 14, 2, 2));
                g.draw(new RoundRectangle2D.Double(7.5, 2.5, 5, 3, 1.5, 1.5));
                g.draw(new Line2D.Double(7, 10, 13, 10));
                g.draw(new Line2D.Double(7, 13.5, 11.5, 13.5));
                break;
            case REFRESH:
                g.draw(new Arc2D.Double(4, 4, 12, 12, 60, 290, Arc2D.OPEN));
                g.draw(new Line2D.Double(13.5, 3, 13.5, 6.5));
                g.draw(new Line2D.Double(13.5, 6.5, 10, 6.5));
                break;
            default:
                break;
        }
    }
}
