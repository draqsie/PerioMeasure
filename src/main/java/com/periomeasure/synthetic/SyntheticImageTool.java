package com.periomeasure.synthetic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a standard set of synthetic test images (PNG + ground-truth CSV)
 * for trying the plugin in Fiji without patient photographs.
 *
 * <pre>
 *   java -cp target/classes com.periomeasure.synthetic.SyntheticImageTool [outputDir]
 * </pre>
 * Default output directory: {@code output/synthetic} (excluded from git).
 */
public final class SyntheticImageTool {

    public static final String DEFAULT_DIR = "output/synthetic";

    private SyntheticImageTool() {
    }

    /** @return name -> scene, in a fixed order; every image is at least 2000 px wide */
    public static Map<String, SyntheticScene> standardScenes() {
        Map<String, SyntheticScene> m = new LinkedHashMap<>();
        m.put("01_typical_frontal", SyntheticScene.of(FaceModel.typical(), 0.05, 0, false, 1.0));
        m.put("02_typical_roll7_oblique", SyntheticScene.of(FaceModel.typical(), 0.05, 7, false, 0.95));
        m.put("03_right_ptosis", SyntheticScene.of(FaceModel.rightPtosis(), 0.045, -4, false, 0.99));
        m.put("04_asymmetric_mirrored", SyntheticScene.of(FaceModel.asymmetric(), 0.05, 3, true, 0.98));
        return Collections.unmodifiableMap(m);
    }

    /** @return all written files ([png, csv] per scene) */
    public static List<File> writeStandardSet(File dir) throws IOException {
        List<File> out = new ArrayList<>();
        for (Map.Entry<String, SyntheticScene> e : standardScenes().entrySet()) {
            out.addAll(SyntheticImageWriter.write(e.getValue(), dir, e.getKey()));
        }
        return out;
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args.length > 0 ? args[0] : DEFAULT_DIR);
        for (File f : writeStandardSet(dir)) {
            System.out.println(f.getPath());
        }
    }
}
