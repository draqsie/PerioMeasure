package com.periomeasure.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.periomeasure.core.Vec2;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.session.Handle;

/**
 * The large photo view. One click places a point; pressing on a point and
 * dragging moves it; dragging empty space pans; the mouse wheel zooms at the
 * cursor. Painting uses {@link ImagePainter} and {@link OverlayPainter} with
 * the same {@link ViewState} that maps clicks.
 */
final class ImageViewer extends JComponent {

    private static final long serialVersionUID = 1L;
    /** A press within this many screen pixels of a point grabs it. */
    static final int HIT_RADIUS = 8;
    private static final int PAN_THRESHOLD = 4;
    private static final Color BACKGROUND = new Color(40, 42, 46);

    private final transient MainWindow window;
    private transient ImagePyramid pyramid;
    private transient ViewState view;
    private boolean userMoved;
    private transient Vec2 hover;

    private int pressX;
    private int pressY;
    private int lastX;
    private int lastY;
    private boolean panning;
    private boolean pointDrag;

    ImageViewer(MainWindow window) {
        this.window = window;
        setPreferredSize(new Dimension(1000, 750));
        setFocusable(true);
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                onPress(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                onDrag(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                onRelease(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setHover(e.getX(), e.getY());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = null;
                window.hoverChanged();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (view() != null) {
                    view = view.zoomAt(Math.pow(1.25, -e.getPreciseWheelRotation()), e.getX(), e.getY());
                    userMoved = true;
                    setHover(e.getX(), e.getY());
                    repaint();
                }
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!userMoved) {
                    view = null;
                    repaint();
                }
            }
        });
    }

    void setImage(ImagePyramid p) {
        this.pyramid = p;
        this.view = null;
        this.userMoved = false;
        this.hover = null;
        repaint();
    }

    ImagePyramid pyramid() {
        return pyramid;
    }

    /** @return the current view, fitted lazily once the component has a size; null without image */
    ViewState view() {
        if (view == null && pyramid != null && getWidth() > 0 && getHeight() > 0) {
            view = ViewState.fit(pyramid.width(), pyramid.height(), getWidth(), getHeight());
        }
        return view;
    }

    Vec2 hover() {
        return hover;
    }

    /** Forgets the cursor position (the loupe then shows the selected point). */
    void clearHover() {
        hover = null;
    }

    void fit() {
        view = null;
        userMoved = false;
        repaint();
    }

    void zoom(double factor) {
        if (view() != null) {
            view = view.zoomAt(factor, getWidth() / 2, getHeight() / 2);
            userMoved = true;
            repaint();
        }
    }

    private boolean measuring() {
        return window.model().phase() == AppModel.Phase.MEASURING;
    }

    private boolean inside(Vec2 p) {
        return p.x >= 0 && p.y >= 0 && p.x <= pyramid.width() && p.y <= pyramid.height();
    }

    private void setHover(int sx, int sy) {
        if (view() == null) {
            return;
        }
        Vec2 p = view.screenToImage(sx, sy);
        hover = inside(p) ? p : null;
        setCursor(Cursor.getPredefinedCursor(measuring() && hover != null ? Cursor.CROSSHAIR_CURSOR
                : Cursor.DEFAULT_CURSOR));
        window.hoverChanged();
    }

    private void onPress(MouseEvent e) {
        requestFocusInWindow();
        pressX = e.getX();
        pressY = e.getY();
        lastX = pressX;
        lastY = pressY;
        panning = false;
        pointDrag = false;
        if (view() == null || !SwingUtilities.isLeftMouseButton(e) || !measuring()) {
            return;
        }
        Handle h = window.model().hit(view.screenToImage(e.getX(), e.getY()), HIT_RADIUS / view.scale);
        if (h != null) {
            window.model().startDrag(h);
            pointDrag = true;
            window.refresh();
        }
    }

    private void onDrag(MouseEvent e) {
        if (view() == null) {
            return;
        }
        if (pointDrag) {
            window.model().dragTo(view.screenToImage(e.getX(), e.getY()));
            setHover(e.getX(), e.getY());
            repaint();
            return;
        }
        if (!panning && Math.hypot(e.getX() - pressX, e.getY() - pressY) > PAN_THRESHOLD) {
            panning = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        if (panning) {
            view = view.pan(e.getX() - lastX, e.getY() - lastY);
            userMoved = true;
            lastX = e.getX();
            lastY = e.getY();
            repaint();
        }
    }

    private void onRelease(MouseEvent e) {
        if (pointDrag) {
            window.model().endDrag();
            pointDrag = false;
            window.refresh();
            return;
        }
        if (panning) {
            panning = false;
            setHover(e.getX(), e.getY());
            return;
        }
        if (view() != null && SwingUtilities.isLeftMouseButton(e) && measuring()) {
            Vec2 p = view.screenToImage(e.getX(), e.getY());
            if (inside(p)) {
                window.placed(window.model().place(p));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(BACKGROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (pyramid == null || view() == null) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(170, 170, 175));
            g2.setFont(getFont().deriveFont(Font.PLAIN, 18f));
            String s = Messages.get(window.model().settings().language, Msg.VIEWER_EMPTY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, getHeight() / 2);
            return;
        }
        ImagePainter.draw(g2, pyramid, view);
        AppModel m = window.model();
        if (m.session() != null) {
            OverlayPainter.paint(g2, m.session(), view, m.selected(), m.phase() == AppModel.Phase.RESULTS,
                    pyramid.width(), pyramid.height());
        }
    }
}
