package com.periomeasure.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.core.Vec2;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

/**
 * Magnifier ({@code zoomFactor}x, 4-8). Shows the area under the cursor while
 * it is over the photo; otherwise the selected point, which can then be
 * dragged here at sub-pixel resolution (1/zoom image pixel). The view is
 * locked during such a drag so the mapping stays fixed.
 */
final class LoupeView extends JComponent {

    private static final long serialVersionUID = 1L;

    private final transient MainWindow window;
    /** Odd edge length, so the centre screen pixel maps exactly to the centre point. */
    private final int size;
    private transient ViewState locked;

    LoupeView(MainWindow window) {
        this.window = window;
        int s = UIScale.scale(260);
        this.size = s % 2 == 0 ? s + 1 : s;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                AppModel model = window.model();
                if (!SwingUtilities.isLeftMouseButton(e) || model.phase() != AppModel.Phase.MEASURING
                        || model.selected() == null) {
                    return;
                }
                Vec2 sel = model.loupeCenter(null);
                locked = ViewState.centeredOn(sel, zoom(), size, size);
                model.beginRefine();
                model.refineTo(locked.screenToImage(e.getX(), e.getY()));
                window.refreshViews();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (locked != null) {
                    window.model().refineTo(locked.screenToImage(e.getX(), e.getY()));
                    window.refreshViews();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (locked != null) {
                    window.model().endDrag();
                    locked = null;
                    window.refresh();
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    int edge() {
        return size;
    }

    private int zoom() {
        return window.model().settings().zoomFactor;
    }

    /** @return the loupe's current view, or null if there is nothing to show */
    ViewState view() {
        if (locked != null) {
            return locked;
        }
        Vec2 c = window.model().loupeCenter(window.viewer().hover());
        return c == null ? null : ViewState.centeredOn(c, zoom(), size, size);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Themes.photoBackground());
        g2.fillRect(0, 0, size, size);
        ImagePyramid p = window.viewer().pyramid();
        ViewState v = view();
        if (p == null || v == null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(190, 190, 195));
            String s = Messages.get(window.model().settings().language, Msg.LOUPE_EMPTY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, Math.max(4, (size - fm.stringWidth(s)) / 2), size / 2);
        } else {
            ImagePainter.draw(g2, p, v);
            AppModel m = window.model();
            if (m.session() != null) {
                OverlayPainter.paint(g2, m.session(), v, m.selected(), m.phase() == AppModel.Phase.RESULTS,
                        p.width(), p.height());
            }
            int c = size / 2;
            int gap = UIScale.scale(7);
            g2.setColor(new Color(255, 60, 60, 170));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(0, c, c - gap, c);
            g2.drawLine(c + gap, c, size, c);
            g2.drawLine(c, 0, c, c - gap);
            g2.drawLine(c, c + gap, c, size);
        }
        g2.setColor(Themes.cardBorder());
        g2.drawRect(0, 0, size - 1, size - 1);
    }
}
