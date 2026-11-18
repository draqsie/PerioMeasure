package com.periomeasure.web;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.JSExport;

import com.periomeasure.Version;
import com.periomeasure.core.EllipseFitResult;
import com.periomeasure.core.EllipseFitter;
import com.periomeasure.core.Vec2;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;

/** Trial: does TeaVM translate the calculation code? */
public final class WebApi {

    private WebApi() {
    }

    @JSExport
    public static String version() {
        return Version.get();
    }

    @JSExport
    public static String text() {
        return Messages.get(Language.TR, Msg.TB_PASTE);
    }

    @JSExport
    public static String fitCircle(double cx, double cy, double r) {
        List<Vec2> pts = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double a = 2 * Math.PI * i / 12;
            pts.add(new Vec2(cx + r * Math.cos(a), cy + r * Math.sin(a)));
        }
        EllipseFitResult f = EllipseFitter.fit(pts);
        return f.ellipse.cx + "," + f.ellipse.cy + "," + f.ellipse.a + "," + f.ellipse.b;
    }
}
