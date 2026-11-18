package com.periomeasure.io;

import java.util.Locale;

import com.periomeasure.core.Quantity;
import com.periomeasure.core.Unit;
import com.periomeasure.i18n.Language;

/**
 * Number formatting for the user interface. Turkish uses a decimal comma,
 * English a decimal point; no digit grouping. Never depends on the JVM's
 * default locale. (CSV formatting is separate, see {@link ResultCsv}.)
 */
public final class Formats {

    private Formats() {
    }

    public static String number(double v, int decimals, Language lang) {
        String s = String.format(Locale.ROOT, "%." + decimals + "f", v);
        if (s.startsWith("-") && Double.parseDouble(s) == 0) {
            s = s.substring(1);
        }
        return lang == Language.TR ? s.replace('.', ',') : s;
    }

    /** Display decimals per unit. */
    public static int decimals(Unit unit) {
        switch (unit) {
            case MM:
            case RATIO:
                return 2;
            default:
                return 1;
        }
    }

    public static String unitSuffix(Unit unit) {
        switch (unit) {
            case MM:
                return " mm";
            case MM2:
                return " mm²";
            case DEGREE:
                return "°";
            default:
                return "";
        }
    }

    /** e.g. {@code 28,41 ± 0,07 mm} */
    public static String quantity(Quantity q, Language lang) {
        int d = decimals(q.unit);
        return number(q.value, d, lang) + " ± " + number(q.sigmaTotal(), d, lang) + unitSuffix(q.unit);
    }

    /** e.g. {@code 0,35 ± 0,01  (PFH 9,80 mm / PFW 28,00 mm)} */
    public static String pfi(Quantity pfi, Quantity pfh, Quantity pfw, Language lang) {
        return quantity(pfi, lang) + "  (PFH " + number(pfh.value, 2, lang) + " mm / PFW "
                + number(pfw.value, 2, lang) + " mm)";
    }
}
