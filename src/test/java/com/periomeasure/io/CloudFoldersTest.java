package com.periomeasure.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CloudFoldersTest {

    private static Map<String, String> env(String oneDrive) {
        Map<String, String> m = new HashMap<>();
        if (oneDrive != null) {
            m.put("OneDrive", oneDrive);
        }
        return m;
    }

    @Test
    void pathUnderTheOneDriveEnvironmentFolderIsDetected() {
        File f = new File("C:\\Users\\A\\OneDrive\\Desktop\\PerioMeasure\\samples\\x.jpg");
        assertEquals("OneDrive", CloudFolders.provider(f, env("C:\\Users\\A\\OneDrive")));
        assertEquals("OneDrive", CloudFolders.provider(new File("c:\\users\\a\\onedrive\\x.jpg"),
                env("C:\\Users\\A\\OneDrive")));
    }

    @Test
    void wellKnownProviderFolderNamesAreDetectedWithoutEnvironment() {
        assertEquals("OneDrive", CloudFolders.provider(new File("D:\\OneDrive - Hastane\\x.jpg"), env(null)));
        assertEquals("Dropbox", CloudFolders.provider(new File("C:\\Users\\A\\Dropbox\\x.jpg"), env(null)));
        assertEquals("Google Drive", CloudFolders.provider(new File("G:\\My Drive\\Google Drive\\x.jpg"), env(null)));
        assertEquals("iCloud", CloudFolders.provider(new File("C:\\Users\\A\\iCloudDrive\\x.jpg"), env(null)));
    }

    @Test
    void localFoldersAreNotFlagged() {
        assertNull(CloudFolders.provider(new File("C:\\dev\\PerioMeasure\\samples\\x.jpg"), env("C:\\Users\\A\\OneDrive")));
        assertNull(CloudFolders.provider(null, env(null)));
    }
}
