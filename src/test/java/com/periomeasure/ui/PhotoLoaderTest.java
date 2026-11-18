package com.periomeasure.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.periomeasure.core.Warning;
import com.periomeasure.core.WarningCode;
import com.periomeasure.io.ExifTestAccess;

class PhotoLoaderTest {

    private static final Map<String, String> NO_ENV = Collections.emptyMap();

    @Test
    void phonePhotoIsTurnedUprightAndItsDateIsRead() throws Exception {
        File f = ExifTestAccess.jpeg("portrait.jpg", 6, "2026:03:02 10:11:12");
        PhotoLoader.Loaded l = PhotoLoader.load(f, NO_ENV);
        assertEquals(4, l.image.getWidth()); // 8 x 4 stored, shown as 4 x 8
        assertEquals(8, l.image.getHeight());
        assertEquals(LocalDate.of(2026, 3, 2), l.dateTaken);
        boolean found = false;
        for (Warning w : l.warnings) {
            found |= w.code == WarningCode.EXIF_ORIENTATION_APPLIED && w.value == 6;
        }
        assertTrue(found, l.warnings.toString());
    }

    @Test
    void uprightPhotoIsLoadedAsIs() throws Exception {
        PhotoLoader.Loaded l = PhotoLoader.load(ExifTestAccess.jpeg("upright.jpg", 1, "2026:01:05 08:00:00"), NO_ENV);
        assertEquals(8, l.image.getWidth());
        assertEquals(4, l.image.getHeight());
        assertTrue(l.warnings.stream().noneMatch(w -> w.code == WarningCode.EXIF_ORIENTATION_APPLIED));
    }

    @Test
    void heicIsRejectedWithAnExplanation() {
        PhotoLoader.LoadException e = assertThrows(PhotoLoader.LoadException.class,
                () -> PhotoLoader.load(new File("IMG_0001.HEIC"), NO_ENV));
        assertTrue(e.heic);
    }

    @Test
    void nonImageFilesAreRejected() throws Exception {
        File dir = new File("target/test-output/loader");
        dir.mkdirs();
        File txt = new File(dir, "notes.jpg");
        Files.write(txt.toPath(), "not an image".getBytes("UTF-8"));
        PhotoLoader.LoadException e = assertThrows(PhotoLoader.LoadException.class,
                () -> PhotoLoader.load(txt, NO_ENV));
        assertTrue(!e.heic);
    }
}
