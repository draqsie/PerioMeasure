package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Quantity;
import com.periomeasure.core.Unit;
import com.periomeasure.i18n.Language;

class FormatsTest {

    @Test
    void decimalSeparatorFollowsTheUiLanguage() {
        assertEquals("1234,57", Formats.number(1234.5678, 2, Language.TR));
        assertEquals("1234.57", Formats.number(1234.5678, 2, Language.EN));
        assertEquals("-0,5", Formats.number(-0.5, 1, Language.TR));
    }

    @Test
    void negativeZeroIsNeverShown() {
        assertEquals("0,00", Formats.number(-0.0001, 2, Language.TR));
        assertEquals("0.0", Formats.number(-0.0, 1, Language.EN));
    }

    @Test
    void quantitiesShowValuePlusMinusTotalSigmaAndUnit() {
        assertEquals("28,41 ± 0,07 mm", Formats.quantity(new Quantity(28.4123, 0.05, 0.05, Unit.MM), Language.TR));
        assertEquals("28.41 ± 0.07 mm", Formats.quantity(new Quantity(28.4123, 0.05, 0.05, Unit.MM), Language.EN));
        assertEquals("0,35 ± 0,01", Formats.quantity(new Quantity(0.3512, 0.006, 0, Unit.RATIO), Language.TR));
        assertEquals("4,3 ± 1,4°", Formats.quantity(new Quantity(4.289, 1.43, 0, Unit.DEGREE), Language.TR));
        assertEquals("105,3 ± 2,1 mm²", Formats.quantity(new Quantity(105.31, 2.1, 0, Unit.MM2), Language.TR));
        assertEquals("-1,00 ± 0,10 mm", Formats.quantity(new Quantity(-1.0, 0.1, 0, Unit.MM), Language.TR));
    }

    @Test
    void pfiLineShowsTheRatioWithItsLengths() {
        Quantity pfi = new Quantity(0.35, 0.01, 0, Unit.RATIO);
        Quantity pfh = new Quantity(9.8, 0.05, 0, Unit.MM);
        Quantity pfw = new Quantity(28.0, 0.05, 0, Unit.MM);
        assertEquals("0,35 ± 0,01  (PFH 9,80 mm / PFW 28,00 mm)", Formats.pfi(pfi, pfh, pfw, Language.TR));
    }
}
