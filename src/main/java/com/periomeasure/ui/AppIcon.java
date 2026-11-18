package com.periomeasure.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * The PerioMeasure application icon, drawn in code so window, taskbar and
 * desktop shortcut always match: a teal rounded tile, a stylised eye (white
 * fissure, iris, pupil, corneal light reflex) and a yellow measuring line
 * from canthus to canthus. Small sizes drop the fine details.
 *
 * <p>{@code java ... com.periomeasure.ui.AppIcon <file.ico> [preview.png]}
 * writes a Windows .ico (PNG entries) and optionally a desktop preview.
 */
public final class AppIcon {

    /** Sizes Windows asks for (taskbar, title bar, Explorer, desktop at 100-200 % scaling). */
    static final int[] SIZES = {16, 20, 24, 32, 40, 48, 64, 96, 128, 256};

    private static final Color TILE_TOP = new Color(0x2A93A6);
    private static final Color TILE_BOTTOM = new Color(0x135A68);
    private static final Color OUTLINE = new Color(0x0E3A44);
    private static final Color IRIS_INNER = new Color(0x4FB3C4);
    private static final Color IRIS_OUTER = new Color(0x15505D);
    private static final Color PUPIL = new Color(0x0A171B);
    private static final Color MEASURE = new Color(0xF6C343);

    private AppIcon() {
    }

    /** @return the icon at every size in {@link #SIZES}, for {@code Window.setIconImages} */
    static List<BufferedImage> images() {
        List<BufferedImage> l = new ArrayList<>();
        for (int s : SIZES) {
            l.add(render(s));
        }
        return l;
    }

    /** Draws the icon at {@code size} x {@code size} pixels (transparent outside the tile). */
    static BufferedImage render(int size) {
        return draw(size, false);
    }

    /**
     * Full-bleed, fully opaque square for iPhone / iPad home screens: iOS
     * rounds the corners itself and would fill transparent corners with black.
     */
    static BufferedImage renderSquare(int size) {
        return draw(size, true);
    }

    private static BufferedImage draw(int size, boolean square) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.scale(size, size); // draw in unit coordinates
            double px = 1.0 / size; // one pixel, for minimum line widths
            boolean small = size <= 24;

            double m = 0.03;
            Shape tile = square ? new java.awt.geom.Rectangle2D.Double(0, 0, 1, 1)
                    : new RoundRectangle2D.Double(m, m, 1 - 2 * m, 1 - 2 * m, 0.44, 0.44);
            g.setPaint(new GradientPaint(0, 0, TILE_TOP, 0, 1, TILE_BOTTOM));
            g.fill(tile);

            Path2D eye = new Path2D.Double();
            eye.moveTo(0.14, 0.46);
            eye.quadTo(0.50, 0.13, 0.86, 0.46);
            eye.quadTo(0.50, 0.80, 0.14, 0.46);
            eye.closePath();
            g.setColor(Color.WHITE);
            g.fill(eye);

            Shape oldClip = g.getClip();
            g.clip(eye);
            double cx = 0.5;
            double cy = 0.46;
            double r = 0.17;
            g.setPaint(new RadialGradientPaint((float) cx, (float) cy, (float) r, new float[] {0f, 1f},
                    new Color[] {IRIS_INNER, IRIS_OUTER}));
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            g.setColor(PUPIL);
            double p = small ? 0.085 : 0.075;
            g.fill(new Ellipse2D.Double(cx - p, cy - p, 2 * p, 2 * p));
            g.setClip(oldClip);
            if (size >= 20) {
                double f = 0.03;
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Double(cx + 0.045 - f, cy - 0.05 - f, 2 * f, 2 * f));
            }
            if (!small) {
                g.setColor(OUTLINE);
                g.setStroke(new BasicStroke((float) Math.max(0.018, px), BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g.draw(eye);
            }

            g.setColor(MEASURE);
            double y = 0.73;
            g.setStroke(new BasicStroke((float) Math.max(0.042, 1.6 * px), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(0.16, y, 0.84, y));
            if (!small) {
                g.draw(new Line2D.Double(0.16, y - 0.06, 0.16, y + 0.06));
                g.draw(new Line2D.Double(0.84, y - 0.06, 0.84, y + 0.06));
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /** Writes a Windows .ico holding every size as a PNG entry (Windows Vista and later). */
    static void writeIco(File file) throws IOException {
        List<byte[]> pngs = new ArrayList<>();
        for (int s : SIZES) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (!ImageIO.write(render(s), "png", bos)) {
                throw new IOException("no PNG writer");
            }
            pngs.add(bos.toByteArray());
        }
        int headerSize = 6 + 16 * SIZES.length;
        ByteBuffer dir = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN);
        dir.putShort((short) 0).putShort((short) 1).putShort((short) SIZES.length);
        int offset = headerSize;
        for (int i = 0; i < SIZES.length; i++) {
            int s = SIZES[i];
            dir.put((byte) (s >= 256 ? 0 : s)).put((byte) (s >= 256 ? 0 : s)); // 0 means 256
            dir.put((byte) 0).put((byte) 0); // no palette, reserved
            dir.putShort((short) 1).putShort((short) 32); // planes, bits per pixel
            dir.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            out.write(dir.array());
            for (byte[] png : pngs) {
                out.write(png);
            }
        }
    }

    /** A picture of the icon on a desktop (light and dark wallpaper) and in the taskbar, for review. */
    static BufferedImage preview() {
        int w = 900;
        int h = 420;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, new Color(0x8FB8DE), w / 2f, h, new Color(0x3C6E9E)));
            g.fillRect(0, 0, w / 2, h - 48);
            g.setPaint(new GradientPaint(w / 2f, 0, new Color(0x1E2533), w, h, new Color(0x0B0F17)));
            g.fillRect(w / 2, 0, w / 2, h - 48);
            Font label = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
            for (int side = 0; side < 2; side++) {
                int x0 = side * w / 2 + 40;
                desktopIcon(g, x0, 30, 48, label);
                desktopIcon(g, x0 + 120, 30, 64, label);
                desktopIcon(g, x0 + 250, 30, 96, label);
                g.drawImage(render(256), x0 + 95, 175, 160, 160, null);
            }
            g.setColor(new Color(0x202020));
            g.fillRect(0, h - 48, w, 48);
            g.setColor(new Color(0x3A3A3A));
            g.fillRoundRect(w / 2 - 24, h - 44, 40, 40, 6, 6);
            g.drawImage(render(32), w / 2 - 20, h - 40, null);
            g.setColor(new Color(0x7FB2FF));
            g.fillRoundRect(w / 2 - 12, h - 5, 16, 3, 3, 3);
        } finally {
            g.dispose();
        }
        return img;
    }

    private static void desktopIcon(Graphics2D g, int x, int y, int s, Font font) {
        g.drawImage(render(s), x + (96 - s) / 2, y + (96 - s), null);
        g.setFont(font);
        String t = "PerioMeasure";
        int tw = g.getFontMetrics().stringWidth(t);
        int tx = x + 48 - tw / 2;
        int ty = y + 96 + 18;
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(t, tx + 1, ty + 1);
        g.setColor(Color.WHITE);
        g.drawString(t, tx, ty);
    }

    /** Writes the icons the web page needs (home screen, browser tab) into {@code dir}. */
    static void writeWebIcons(File dir) throws IOException {
        Files.createDirectories(dir.toPath());
        ImageIO.write(renderSquare(180), "png", new File(dir, "apple-touch-icon.png"));
        ImageIO.write(renderSquare(192), "png", new File(dir, "icon-192.png"));
        ImageIO.write(renderSquare(512), "png", new File(dir, "icon-512.png"));
        ImageIO.write(render(32), "png", new File(dir, "favicon-32.png"));
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 2 && "--web".equals(args[0])) {
            writeWebIcons(new File(args[1]));
            return;
        }
        if (args.length < 1 || args.length > 2) {
            System.err.println("usage: AppIcon <file.ico> [preview.png]  |  AppIcon --web <dir>");
            System.exit(2);
        }
        writeIco(new File(args[0]));
        if (args.length == 2) {
            ImageIO.write(preview(), "png", new File(args[1]));
        }
    }
}
