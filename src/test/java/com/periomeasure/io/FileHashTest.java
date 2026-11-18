package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

class FileHashTest {

    @Test
    void sha256OfKnownString() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", FileHash.sha256Hex("abc"));
    }

    @Test
    void onlyTheFileNameIsHashedNotTheFolder() {
        String a = FileHash.fileNameHash(new File("C:\\patients\\abc"));
        String b = FileHash.fileNameHash(new File("D:\\other\\place\\abc"));
        assertEquals(FileHash.sha256Hex("abc"), a);
        assertEquals(a, b);
    }

    @Test
    void turkishFileNamesAreHashedAsUtf8() {
        String h = FileHash.fileNameHash(new File("C:\\x\\Gözkapağı_ön.jpg"));
        assertEquals(64, h.length());
        assertEquals(h.toLowerCase(java.util.Locale.ROOT), h);
        assertNotEquals(FileHash.sha256Hex("Gozkapagi_on.jpg"), h);
    }

    @Test
    void missingFileGivesEmptyHash() {
        assertEquals("", FileHash.fileNameHash(null));
    }
}
