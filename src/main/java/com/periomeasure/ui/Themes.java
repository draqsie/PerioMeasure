package com.periomeasure.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.UIScale;
import com.periomeasure.io.Theme;

/**
 * Light and dark themes (FlatLaf, which ships with Fiji): larger default
 * font, rounded controls, a calm clinical teal accent. FlatLaf also scales
 * the whole interface on high-resolution screens.
 */
final class Themes {

    static final String ACCENT_HEX = "#1F7A8C";
    private static Theme current = Theme.LIGHT;
    private static float baseFontSize = -1;

    private Themes() {
    }

    static void apply(Theme theme) {
        Map<String, String> extra = new HashMap<>();
        extra.put("@accentColor", ACCENT_HEX);
        FlatLaf.setGlobalExtraDefaults(extra);
        if (theme == Theme.DARK) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        Font f = UIManager.getFont("defaultFont");
        if (f != null) {
            if (baseFontSize < 0) {
                baseFontSize = f.getSize2D();
            }
            UIManager.put("defaultFont", new FontUIResource(f.deriveFont(baseFontSize + 3f)));
        }
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Table.rowHeight", UIScale.scale(28));
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("TableHeader.height", UIScale.scale(30));
        current = theme;
        OverlayPainter.uiScale = UIScale.getUserScaleFactor();
        FlatLaf.updateUI();
    }

    static Theme current() {
        return current;
    }

    static boolean dark() {
        return current == Theme.DARK;
    }

    static Color accent() {
        return Color.decode(ACCENT_HEX);
    }

    /** Neutral backdrop behind the photo (dark in both themes, like photo software). */
    static Color photoBackground() {
        return dark() ? new Color(24, 25, 28) : new Color(58, 61, 66);
    }

    static Color muted() {
        Color c = UIManager.getColor("Label.disabledForeground");
        return c != null ? c : Color.GRAY;
    }

    static Color warning() {
        return dark() ? new Color(255, 183, 77) : new Color(170, 80, 0);
    }

    static Color success() {
        return dark() ? new Color(129, 199, 132) : new Color(0, 120, 60);
    }

    static Color error() {
        return dark() ? new Color(239, 154, 154) : new Color(190, 0, 0);
    }

    /** Slightly raised surface for cards on the side panel. */
    static Color card() {
        return dark() ? new Color(49, 52, 56) : new Color(246, 248, 250);
    }

    static Color cardBorder() {
        return dark() ? new Color(70, 74, 80) : new Color(222, 226, 231);
    }

    static float font(float delta) {
        Font f = UIManager.getFont("defaultFont");
        return (f != null ? f.getSize2D() : 15f) + delta;
    }
}
