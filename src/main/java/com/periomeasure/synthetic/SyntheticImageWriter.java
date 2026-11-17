package com.periomeasure.synthetic;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import com.periomeasure.core.Ellipse;
import com.periomeasure.core.Eye;
import com.periomeasure.core.EyeLandmarks;
import com.periomeasure.core.MeasureId;
import com.periomeasure.core.Vec2;

/**
 * Writes a synthetic scene as a lossless PNG plus a ground-truth CSV
 * (UTF-8, comma separated, dot decimal) listing landmark pixel coordinates,
 * scene parameters and the expected measurements.
 */
public final class SyntheticImageWriter {

    private SyntheticImageWriter() {
    }

    /** @return [png, csv] */
    public static List<File> write(SyntheticScene scene, File dir, String baseName) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Cannot create directory " + dir);
        }
        File png = new File(dir, baseName + ".png");
        File csv = new File(dir, baseName + "_truth.csv");
        if (!ImageIO.write(SyntheticRenderer.render(scene), "png", png)) {
            throw new IOException("No PNG writer available");
        }
        try (PrintWriter w = new PrintWriter(new OutputStreamWriter(
                Files.newOutputStream(csv.toPath()), StandardCharsets.UTF_8))) {
            w.print("item,eye,x_px,y_px,value,unit\n");
            for (Eye eye : Eye.values()) {
                EyeLandmarks lm = scene.landmarksPx(eye);
                point(w, "pupil", eye, lm.pupil);
                point(w, "upper_lid", eye, lm.upperLid);
                point(w, "lower_lid", eye, lm.lowerLid);
                point(w, "medial_canthus", eye, lm.medialCanthus);
                point(w, "lateral_canthus", eye, lm.lateralCanthus);
                point(w, "upper_lid_crease", eye, lm.upperLidCrease);
                point(w, "brow_lower_border", eye, lm.browLowerBorder);
                point(w, "limbus_upper", eye, lm.limbusUpper);
                point(w, "limbus_lower", eye, lm.limbusLower);
            }
            Ellipse s = scene.stickerPx();
            point(w, "sticker_center", null, s.center());
            value(w, "sticker_major_axis", null, s.majorAxis(), "px");
            value(w, "sticker_axis_ratio", null, s.axisRatio(), "ratio");
            value(w, "mm_per_px", null, scene.mmPerPx, "mm/px");
            value(w, "roll_deg", null, scene.rollDeg, "deg");
            w.print("mirrored,,,," + scene.mirrored + ",\n");
            GroundTruth truth = GroundTruth.of(scene.face);
            for (Eye eye : Eye.values()) {
                for (Map.Entry<MeasureId, Double> e : truth.perEye(eye).entrySet()) {
                    value(w, e.getKey().name(), eye, e.getValue(), unit(e.getKey()));
                }
            }
            value(w, MeasureId.ICD.name(), null, truth.icd(), "mm");
            value(w, MeasureId.IPD.name(), null, truth.ipd(), "mm");
        }
        return Arrays.asList(png, csv);
    }

    private static String unit(MeasureId id) {
        switch (id.unit) {
            case MM:
                return "mm";
            case MM2:
                return "mm2";
            case DEGREE:
                return "deg";
            default:
                return "ratio";
        }
    }

    private static void point(PrintWriter w, String item, Eye eye, Vec2 p) {
        w.print(item + "," + (eye == null ? "" : eye.name()) + "," + num(p.x) + "," + num(p.y) + ",,px\n");
    }

    private static void value(PrintWriter w, String item, Eye eye, double v, String unit) {
        w.print(item + "," + (eye == null ? "" : eye.name()) + ",,," + num(v) + "," + unit + "\n");
    }

    private static String num(double v) {
        return String.format(Locale.ROOT, "%.9f", v);
    }
}
