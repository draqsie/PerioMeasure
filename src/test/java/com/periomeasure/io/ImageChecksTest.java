package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;

class ImageChecksTest {

    private static final Map<String, String> NO_ENV = Collections.emptyMap();

    private static Warning find(List<Warning> ws, WarningCode code) {
        for (Warning w : ws) {
            if (w.code == code) {
                return w;
            }
        }
        return null;
    }

    @Test
    void narrowImageWarns() {
        assertEquals(1999, find(ImageChecks.check(1999, 1500, null, false, NO_ENV), WarningCode.IMAGE_TOO_NARROW).value, 0);
        assertTrue(ImageChecks.check(2000, 1500, null, false, NO_ENV).isEmpty());
    }

    @Test
    void fileCalibrationIsReportedAsIgnored() {
        assertTrue(find(ImageChecks.check(3000, 2000, null, true, NO_ENV), WarningCode.FILE_CALIBRATION_IGNORED) != null);
    }

    @Test
    void lowJpegQualityWarns() throws Exception {
        File low = JpegQualityTest.writeJpeg(0.5f);
        File high = JpegQualityTest.writeJpeg(0.95f);
        Warning w = find(ImageChecks.check(3000, 2000, low, false, NO_ENV), WarningCode.JPEG_LOW_QUALITY);
        assertEquals(50, w.value, 2);
        assertTrue(find(ImageChecks.check(3000, 2000, high, false, NO_ENV), WarningCode.JPEG_LOW_QUALITY) == null);
    }

    @Test
    void cloudFolderWarnsWithProvider() {
        Map<String, String> env = new HashMap<>();
        env.put("OneDrive", "C:\\Users\\A\\OneDrive");
        Warning w = find(ImageChecks.check(3000, 2000, new File("C:\\Users\\A\\OneDrive\\x.png"), false, env),
                WarningCode.CLOUD_FOLDER_INPUT);
        assertEquals("OneDrive", w.detail);
    }
}
