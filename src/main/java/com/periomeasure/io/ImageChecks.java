package com.periomeasure.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;

/** Checks on the photograph itself, run before measuring. */
public final class ImageChecks {

    /** Images narrower than this trigger a warning (px). */
    public static final int MIN_WIDTH_PX = 2000;
    /** JPEGs with an estimated IJG quality below this trigger a warning. */
    public static final int MIN_JPEG_QUALITY = 85;

    private ImageChecks() {
    }

    /**
     * @param file           the photograph file, or null if unsaved
     * @param fileCalibrated true if the file carries its own spatial calibration
     * @param env            environment variables (for cloud-folder detection)
     */
    public static List<Warning> check(int width, int height, File file, boolean fileCalibrated,
            Map<String, String> env) {
        List<Warning> w = new ArrayList<>();
        if (width < MIN_WIDTH_PX) {
            w.add(new Warning(WarningCode.IMAGE_TOO_NARROW, null, width));
        }
        if (fileCalibrated) {
            w.add(new Warning(WarningCode.FILE_CALIBRATION_IGNORED, null, Double.NaN));
        }
        if (file != null) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                int q = JpegQuality.estimate(file);
                if (q > 0 && q < MIN_JPEG_QUALITY) {
                    w.add(new Warning(WarningCode.JPEG_LOW_QUALITY, null, q));
                }
            }
            String provider = CloudFolders.provider(file, env);
            if (provider != null) {
                w.add(new Warning(WarningCode.CLOUD_FOLDER_INPUT, null, Double.NaN, provider));
            }
        }
        return w;
    }
}
