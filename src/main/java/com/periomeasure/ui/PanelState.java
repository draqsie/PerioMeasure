package com.periomeasure.ui;

import com.periomeasure.core.EllipseFitResult;
import com.periomeasure.i18n.Language;
import com.periomeasure.i18n.Messages;
import com.periomeasure.i18n.Msg;
import com.periomeasure.i18n.UiTexts;
import com.periomeasure.io.Formats;
import com.periomeasure.session.MeasurementSession;
import com.periomeasure.session.Step;

/**
 * Everything the control panel shows for the current session state:
 * texts and which buttons are visible/enabled. Pure (no Swing), so it is
 * unit-tested; {@link ControlPanel} only renders it.
 */
public final class PanelState {

    public final String stepLabel;
    public final String title;
    public final String help;
    /** "Points: n (minimum m)" for multi-point steps, else null. */
    public final String pointsText;
    /** Live sticker fit summary (5 or more sticker points), else null. */
    public final String calibrationText;
    /** Shown when the last step is reached and everything is complete, else null. */
    public final String readyText;
    public final boolean previousEnabled;
    public final boolean nextVisible;
    public final boolean nextEnabled;
    public final boolean skipVisible;
    public final boolean finishVisible;
    public final boolean finishEnabled;

    private PanelState(MeasurementSession s, Language l, int zoom) {
        Step st = s.current();
        stepLabel = Messages.get(l, Msg.PANEL_STEP, s.currentIndex() + 1, s.steps().size());
        String t = UiTexts.stepTitle(l, st.landmark);
        title = st.eye == null ? t : UiTexts.eye(l, st.eye) + " · " + t;
        help = UiTexts.stepHelp(l, st.landmark) + "\n\n" + Messages.get(l, Msg.PANEL_HELP, zoom);
        pointsText = st.landmark.isMultiPoint()
                ? Messages.get(l, Msg.PANEL_POINTS, s.points(s.currentIndex()).size(), st.landmark.minPoints)
                : null;
        EllipseFitResult fit = s.liveFit();
        calibrationText = fit == null ? null
                : Messages.get(l, Msg.PANEL_CAL_LIVE, Formats.number(fit.ellipse.majorAxis(), 1, l),
                        Formats.number(fit.ellipse.axisRatio(), 3, l), Formats.number(fit.rmsResidualPx, 2, l));
        previousEnabled = s.currentIndex() > 0;
        nextVisible = !s.isLast();
        nextEnabled = s.canAdvance();
        skipVisible = st.optional;
        finishVisible = s.isLast();
        finishEnabled = s.canFinish();
        readyText = s.isLast() && s.canFinish() ? Messages.get(l, Msg.PANEL_READY) : null;
    }

    public static PanelState of(MeasurementSession s, Language l, int zoom) {
        return new PanelState(s, l, zoom);
    }
}
