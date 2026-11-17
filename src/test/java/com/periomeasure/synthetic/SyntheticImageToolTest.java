package com.periomeasure.synthetic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

class SyntheticImageToolTest {

    @Test
    void writesTheStandardSetOfImagesWithGroundTruth() throws Exception {
        File dir = new File("target/test-output/synthetic-set");
        List<File> files = SyntheticImageTool.writeStandardSet(dir);
        assertEquals(2 * SyntheticImageTool.standardScenes().size(), files.size());
        for (File f : files) {
            assertTrue(f.isFile() && f.length() > 0, f.toString());
            assertTrue(f.getCanonicalPath().startsWith(dir.getCanonicalPath()), f.toString());
        }
        // Every standard image is wide enough not to trigger the < 2000 px warning.
        for (SyntheticScene s : SyntheticImageTool.standardScenes().values()) {
            assertTrue(s.width >= 2000, "width " + s.width);
        }
    }
}
