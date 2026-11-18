package com.periomeasure.i18n;

/**
 * Every user-visible text key. Using an enum (instead of free strings) means a
 * typo in a key is a compile error, and {@code MessagesTest} guarantees each
 * constant has a non-empty text in every {@link Language}. Constants named
 * {@code STEP_<LANDMARK>_TITLE/_HELP}, {@code MEASURE_<ID>} and
 * {@code WARN_<CODE>} are looked up by name in {@link UiTexts}.
 */
public enum Msg {
    ABOUT_TITLE("about.title"),
    ABOUT_BODY("about.body"),

    // ---- main window
    APP_TITLE("app.title"),
    TB_OPEN("tb.open"),
    TB_DEMO("tb.demo"),
    TB_FIT("tb.fit"),
    TB_ZOOM_IN("tb.zoomIn"),
    TB_ZOOM_OUT("tb.zoomOut"),
    TB_SETTINGS("tb.settings"),
    TB_ABOUT("tb.about"),
    WELCOME_TITLE("welcome.title"),
    WELCOME_TEXT("welcome.text"),
    VIEWER_EMPTY("viewer.empty"),
    LOUPE_HINT("loupe.hint"),
    LOUPE_EMPTY("loupe.empty"),
    STATUS("status"),
    LEGEND("legend"),
    LOADING("loading"),
    CONFIRM_DISCARD("confirm.discard"),
    CHOOSER_TITLE("chooser.title"),
    CHOOSER_FILTER("chooser.filter"),

    // ---- setup form
    SETUP_TITLE("setup.title"),
    SETUP_PHOTO("setup.photo"),
    SETUP_PATIENT("setup.patient"),
    SETUP_DATE("setup.date"),
    SETUP_EYE("setup.eye"),
    EYES_BOTH("eyes.both"),
    EYES_OD("eyes.od"),
    EYES_OS("eyes.os"),
    SETUP_TIMING("setup.timing"),
    TIMING_PRE("timing.pre"),
    TIMING_POST("timing.post"),
    SETUP_AXIS("setup.axis"),
    AXIS_IMAGE("axis.image"),
    AXIS_LINE("axis.line"),
    SETUP_OPTIONAL("setup.optional"),
    OPT_CREASE("opt.crease"),
    OPT_BROW("opt.brow"),
    OPT_LIMBUS("opt.limbus"),
    OPT_CONTOUR("opt.contour"),
    SETUP_STICKER("setup.sticker"),
    SETUP_PRIVACY("setup.privacy"),
    SETUP_START("setup.start"),
    PANEL_IMAGE_WARNINGS("panel.imageWarnings"),

    // ---- errors
    ERR_TITLE("err.title"),
    ERR_PATIENT("err.patient"),
    ERR_DATE("err.date"),
    ERR_COMPUTE("err.compute"),
    ERR_SAVE("err.save"),
    ERR_DIAMETER("err.diameter"),
    ERR_HEIC("err.heic"),
    ERR_UNSUPPORTED("err.unsupported"),

    // ---- step panel
    PANEL_STEP("panel.step"),
    PANEL_HELP("panel.help"),
    PANEL_POINTS("panel.points"),
    PANEL_CAL_LIVE("panel.calLive"),
    PANEL_READY("panel.ready"),
    BTN_PREVIOUS("btn.previous"),
    BTN_UNDO("btn.undo"),
    BTN_SKIP("btn.skip"),
    BTN_NEXT("btn.next"),
    BTN_FINISH("btn.finish"),
    BTN_CANCEL("btn.cancel"),
    BTN_YES("btn.yes"),
    BTN_NO("btn.no"),
    EYE_OD("eye.od"),
    EYE_OS("eye.os"),

    // ---- steps (title + help per landmark)
    STEP_CALIBRATION_EDGE_TITLE("step.calibration.title"),
    STEP_CALIBRATION_EDGE_HELP("step.calibration.help"),
    STEP_REFERENCE_LINE_TITLE("step.line.title"),
    STEP_REFERENCE_LINE_HELP("step.line.help"),
    STEP_PUPIL_TITLE("step.pupil.title"),
    STEP_PUPIL_HELP("step.pupil.help"),
    STEP_UPPER_LID_TITLE("step.upperLid.title"),
    STEP_UPPER_LID_HELP("step.upperLid.help"),
    STEP_LOWER_LID_TITLE("step.lowerLid.title"),
    STEP_LOWER_LID_HELP("step.lowerLid.help"),
    STEP_MEDIAL_CANTHUS_TITLE("step.mc.title"),
    STEP_MEDIAL_CANTHUS_HELP("step.mc.help"),
    STEP_LATERAL_CANTHUS_TITLE("step.lc.title"),
    STEP_LATERAL_CANTHUS_HELP("step.lc.help"),
    STEP_UPPER_LID_CREASE_TITLE("step.crease.title"),
    STEP_UPPER_LID_CREASE_HELP("step.crease.help"),
    STEP_BROW_LOWER_BORDER_TITLE("step.brow.title"),
    STEP_BROW_LOWER_BORDER_HELP("step.brow.help"),
    STEP_LIMBUS_UPPER_TITLE("step.limbusUpper.title"),
    STEP_LIMBUS_UPPER_HELP("step.limbusUpper.help"),
    STEP_LIMBUS_LOWER_TITLE("step.limbusLower.title"),
    STEP_LIMBUS_LOWER_HELP("step.limbusLower.help"),
    STEP_CONTOUR_TITLE("step.contour.title"),
    STEP_CONTOUR_HELP("step.contour.help"),

    // ---- measurement names
    MEASURE_PFI("measure.PFI"),
    MEASURE_ICD("measure.ICD"),
    MEASURE_PFW("measure.PFW"),
    MEASURE_PFH("measure.PFH"),
    MEASURE_MRD1("measure.MRD1"),
    MEASURE_MRD2("measure.MRD2"),
    MEASURE_CANTHAL_ANGLE("measure.CANTHAL_ANGLE"),
    MEASURE_IPD("measure.IPD"),
    MEASURE_FISSURE_AREA("measure.FISSURE_AREA"),
    MEASURE_TPS("measure.TPS"),
    MEASURE_BROW_HEIGHT("measure.BROW_HEIGHT"),
    MEASURE_SUPERIOR_SCLERAL_SHOW("measure.SSS"),
    MEASURE_INFERIOR_SCLERAL_SHOW("measure.ISS"),

    // ---- warnings ({0} value, {1} eye, {2} detail)
    WARN_OBLIQUE_VIEW("warn.obliqueView"),
    WARN_LOW_RESOLUTION("warn.lowResolution"),
    WARN_IMAGE_MIRRORED("warn.imageMirrored"),
    WARN_CANTHUS_ORDER("warn.canthusOrder"),
    WARN_MRD_INCONSISTENT("warn.mrdInconsistent"),
    WARN_NONPOSITIVE_PFH("warn.nonpositivePfh"),
    WARN_LID_POINT_OFF_PUPIL_AXIS("warn.lidOffAxis"),
    WARN_IMAGE_TOO_NARROW("warn.imageTooNarrow"),
    WARN_JPEG_LOW_QUALITY("warn.jpegLowQuality"),
    WARN_FILE_CALIBRATION_IGNORED("warn.fileCalibrationIgnored"),
    WARN_CLOUD_FOLDER_INPUT("warn.cloudInput"),
    WARN_CLOUD_FOLDER_OUTPUT("warn.cloudOutput"),
    WARN_EXIF_ORIENTATION_APPLIED("warn.exifOrientation"),

    // ---- results
    RES_HEADER("res.header"),
    RES_COL_MEASURE("res.colMeasure"),
    RES_COL_DIFF("res.colDiff"),
    RES_CAL("res.cal"),
    RES_AXIS("res.axis"),
    AXIS_SRC_PUPILS("axisSrc.pupils"),
    AXIS_SRC_USER_LINE("axisSrc.line"),
    AXIS_SRC_IMAGE_HORIZONTAL("axisSrc.image"),
    RES_SAVED("res.saved"),
    RES_WARNINGS("res.warnings"),
    RES_NO_WARNINGS("res.noWarnings"),
    RES_DISCLAIMER("res.disclaimer"),
    RES_VERIFY_TITLE("res.verifyTitle"),
    RES_COL_EYE("res.colEye"),
    RES_COL_MEASURED("res.colMeasured"),
    RES_COL_TRUE("res.colTrue"),
    RES_COL_ERROR("res.colError"),
    RES_COL_WITHIN("res.colWithin"),
    RES_VERIFY_SUMMARY("res.verifySummary"),
    BTN_COPY("btn.copy"),
    BTN_OPEN_FOLDER("btn.openFolder"),
    BTN_NEW_MEASUREMENT("btn.newMeasurement"),
    BTN_OPEN_OTHER("btn.openOther"),

    // ---- demo
    DEMO_SCENE_1("demo.scene1"),
    DEMO_SCENE_2("demo.scene2"),
    DEMO_SCENE_3("demo.scene3"),
    DEMO_SCENE_4("demo.scene4"),

    // ---- settings dialog
    SET_TITLE("set.title"),
    SET_LANGUAGE("set.language"),
    SET_DIAMETER("set.diameter"),
    SET_DECIMAL("set.decimal"),
    DEC_DOT("dec.dot"),
    DEC_COMMA("dec.comma"),
    SET_OUTPUT("set.output"),
    SET_ZOOM("set.zoom");

    private final String key;

    Msg(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
