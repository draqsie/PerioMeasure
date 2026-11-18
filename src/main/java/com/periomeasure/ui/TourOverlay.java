package com.periomeasure.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

/**
 * First-start guide shown on the window's glass pane: the window is dimmed,
 * the control being explained is highlighted, and a large-print callout
 * with an arrow explains it in plain words. Next / Back / Close buttons,
 * arrow keys and Esc work.
 */
final class TourOverlay extends JComponent {

    private static final long serialVersionUID = 1L;

    /** One stop of the tour: what to highlight (null = nothing, centred text) and its texts. */
    static final class Stop {
        final transient Supplier<Component> target;
        final Msg title;
        final Msg body;

        Stop(Supplier<Component> target, Msg title, Msg body) {
            this.target = target;
            this.title = title;
            this.body = body;
        }
    }

    private final transient List<Stop> stops;
    private final transient Language lang;
    private final transient Runnable onClose;
    private final JPanel bubble;
    private final JLabel counter = new JLabel();
    private final JLabel title = new JLabel();
    private final JTextArea body;
    private final JButton back;
    private final JButton next;
    private int index;

    TourOverlay(List<Stop> stops, Language lang, Runnable onClose) {
        this.stops = stops;
        this.lang = lang;
        this.onClose = onClose;
        setLayout(null);
        setOpaque(false);
        MouseAdapter block = new MouseAdapter() {
        };
        addMouseListener(block);
        addMouseMotionListener(block);
        addMouseWheelListener(block);

        bubble = Ui.card(new BorderLayout(0, UIScale.scale(10)));
        counter.setForeground(Themes.muted());
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, Themes.font(6)));
        title.setForeground(Themes.accent());
        body = Ui.text("");
        body.setFont(body.getFont().deriveFont(Themes.font(2)));
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(counter);
        head.add(title);
        back = Ui.button(Messages.get(lang, Msg.TOUR_BACK), e -> go(index - 1));
        next = Ui.primary(Messages.get(lang, Msg.TOUR_NEXT), null, e -> go(index + 1));
        JButton close = Ui.flat(Messages.get(lang, Msg.TOUR_CLOSE), null, null, e -> close());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIScale.scale(6), 0));
        buttons.setOpaque(false);
        buttons.add(close);
        buttons.add(back);
        buttons.add(next);
        bubble.add(head, BorderLayout.NORTH);
        bubble.add(body, BorderLayout.CENTER);
        bubble.add(buttons, BorderLayout.SOUTH);
        add(bubble);

        bindKey("ESCAPE", "tourClose", this::close);
        bindKey("RIGHT", "tourNext", () -> go(index + 1));
        bindKey("ENTER", "tourNext2", () -> go(index + 1));
        bindKey("LEFT", "tourBack", () -> go(index - 1));
    }

    private void bindKey(String key, String name, Runnable r) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (isVisible()) {
                    r.run();
                }
            }
        });
    }

    int index() {
        return index;
    }

    int stopCount() {
        return stops.size();
    }

    void start() {
        setVisible(true);
        go(0);
    }

    void go(int i) {
        if (i < 0) {
            return;
        }
        if (i >= stops.size()) {
            close();
            return;
        }
        index = i;
        Stop s = stops.get(i);
        counter.setText(Messages.get(lang, Msg.TOUR_STEP, i + 1, stops.size()));
        title.setText(Messages.get(lang, s.title));
        body.setText(Messages.get(lang, s.body));
        back.setEnabled(i > 0);
        next.setText(Messages.get(lang, i == stops.size() - 1 ? Msg.TOUR_DONE : Msg.TOUR_NEXT));
        revalidate();
        doLayout();
        repaint();
    }

    void close() {
        setVisible(false);
        onClose.run();
    }

    /** @return the highlighted rectangle in this overlay's coordinates, or null */
    Rectangle hole() {
        if (stops.isEmpty()) {
            return null;
        }
        Supplier<Component> t = stops.get(index).target;
        Component c = t == null ? null : t.get();
        if (c == null || !c.isShowing()) {
            return null;
        }
        Rectangle r = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), this);
        int m = UIScale.scale(6);
        r.grow(m, m);
        return r;
    }

    private boolean large(Rectangle hole) {
        // Only the photo area is "large": the callout then sits inside it. Anything smaller
        // (e.g. the right-hand panel) gets the callout beside it with an arrow.
        return hole != null && (double) hole.width * hole.height > 0.45 * getWidth() * getHeight();
    }

    @Override
    public void doLayout() {
        int bw = Math.min(UIScale.scale(470), Math.max(UIScale.scale(300), getWidth() - UIScale.scale(40)));
        body.setSize(new Dimension(bw - UIScale.scale(30), Short.MAX_VALUE));
        Dimension pref = bubble.getPreferredSize();
        int bh = pref.height;
        Rectangle hole = hole();
        int gap = UIScale.scale(56);
        int x;
        int y;
        if (hole == null || large(hole)) {
            Rectangle area = hole == null ? new Rectangle(0, 0, getWidth(), getHeight()) : hole;
            x = area.x + (area.width - bw) / 2;
            y = area.y + (area.height - bh) / 2;
        } else if (hole.y + hole.height + gap + bh < getHeight()) {
            x = hole.x + hole.width / 2 - bw / 2;
            y = hole.y + hole.height + gap;
        } else if (hole.y - gap - bh > 0) {
            x = hole.x + hole.width / 2 - bw / 2;
            y = hole.y - gap - bh;
        } else {
            x = hole.x - gap - bw;
            y = hole.y + hole.height / 2 - bh / 2;
        }
        int m = UIScale.scale(12);
        x = Math.max(m, Math.min(getWidth() - bw - m, x));
        y = Math.max(m, Math.min(getHeight() - bh - m, y));
        bubble.setBounds(x, y, bw, bh);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle hole = hole();
            Area dim = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
            int arc = UIScale.scale(16);
            if (hole != null) {
                dim.subtract(new Area(new RoundRectangle2D.Double(hole.x, hole.y, hole.width, hole.height, arc, arc)));
            }
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fill(dim);
            if (hole != null) {
                g2.setColor(Themes.accent().brighter());
                g2.setStroke(new BasicStroke(UIScale.scale(3f)));
                g2.draw(new RoundRectangle2D.Double(hole.x, hole.y, hole.width, hole.height, arc, arc));
                if (!large(hole)) {
                    drawArrow(g2, bubble.getBounds(), hole);
                }
            }
        } finally {
            g2.dispose();
        }
    }

    /** Thick arrow from the callout to the highlighted control. */
    private static void drawArrow(Graphics2D g2, Rectangle from, Rectangle to) {
        Point a = edgePoint(from, center(to));
        Point b = edgePoint(to, center(from));
        float w = UIScale.scale(4f);
        g2.setStroke(new BasicStroke(w + UIScale.scale(4f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 255, 200));
        g2.draw(new Line2D.Double(a, b));
        g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 196, 0));
        g2.draw(new Line2D.Double(a, b));
        double ang = Math.atan2(b.y - a.y, b.x - a.x);
        double len = UIScale.scale(18f);
        Path2D head = new Path2D.Double();
        head.moveTo(b.x, b.y);
        head.lineTo(b.x - len * Math.cos(ang - 0.45), b.y - len * Math.sin(ang - 0.45));
        head.lineTo(b.x - len * Math.cos(ang + 0.45), b.y - len * Math.sin(ang + 0.45));
        head.closePath();
        g2.fill(head);
    }

    private static Point center(Rectangle r) {
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    /** Point where the segment from the centre of {@code r} towards {@code p} leaves {@code r}. */
    private static Point edgePoint(Rectangle r, Point p) {
        Point c = center(r);
        double dx = p.x - c.x;
        double dy = p.y - c.y;
        if (dx == 0 && dy == 0) {
            return c;
        }
        double tx = dx == 0 ? Double.MAX_VALUE : (r.width / 2.0) / Math.abs(dx);
        double ty = dy == 0 ? Double.MAX_VALUE : (r.height / 2.0) / Math.abs(dy);
        double t = Math.min(tx, ty);
        return new Point((int) Math.round(c.x + dx * t), (int) Math.round(c.y + dy * t));
    }
}
