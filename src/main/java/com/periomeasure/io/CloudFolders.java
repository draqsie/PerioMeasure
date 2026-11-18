package com.periomeasure.io;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Detects folders that are synchronised to a cloud service, so the user can
 * be warned before patient photographs or results leave the computer.
 * Used only for warnings: false positives are acceptable, false negatives
 * are not.
 */
public final class CloudFolders {

    private static final String[] ONEDRIVE_ENV = {"OneDrive", "OneDriveConsumer", "OneDriveCommercial"};

    private CloudFolders() {
    }

    /**
     * @param f   file or folder (may be null)
     * @param env environment variables (normally {@code System.getenv()})
     * @return provider name ("OneDrive", "Dropbox", "Google Drive", "iCloud", "Box") or null
     */
    public static String provider(File f, Map<String, String> env) {
        if (f == null) {
            return null;
        }
        String path = normalise(f.getAbsolutePath());
        for (String key : ONEDRIVE_ENV) {
            String root = env.get(key);
            if (root != null && !root.trim().isEmpty()) {
                String r = normalise(root.trim());
                if (path.equals(r) || path.startsWith(r + "/")) {
                    return "OneDrive";
                }
            }
        }
        for (String seg : path.split("/")) {
            if (seg.contains("onedrive")) {
                return "OneDrive";
            }
            if (seg.contains("dropbox")) {
                return "Dropbox";
            }
            if (seg.equals("google drive") || seg.contains("googledrive") || seg.equals("my drive")) {
                return "Google Drive";
            }
            if (seg.contains("icloud")) {
                return "iCloud";
            }
            if (seg.equals("box") || seg.equals("box sync")) {
                return "Box";
            }
        }
        return null;
    }

    private static String normalise(String p) {
        String s = p.replace('\\', '/').toLowerCase(Locale.ROOT);
        while (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
