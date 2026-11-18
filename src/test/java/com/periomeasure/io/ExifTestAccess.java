package com.periomeasure.io;

import java.io.File;
import java.nio.ByteOrder;

/** Shares the EXIF fixtures of {@link ExifTest} with tests in other packages. */
public final class ExifTestAccess {

    private ExifTestAccess() {
    }

    /** An 8 x 4 px JPEG whose EXIF says: orientation {@code orientation}, taken on {@code dateTime}. */
    public static File jpeg(String name, int orientation, String dateTime) throws Exception {
        return ExifTest.jpegWithExif(name, ExifTest.tiff(ByteOrder.LITTLE_ENDIAN, orientation, dateTime));
    }
}
