package com.periomeasure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void versionIsFilteredFromPom() {
        String v = Version.get();
        assertFalse(v.contains("${"), "resource filtering did not run: " + v);
        assertTrue(v.matches("\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?"), "unexpected version format: " + v);
    }
}
