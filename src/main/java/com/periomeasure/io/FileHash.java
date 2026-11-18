package com.periomeasure.io;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The CSV stores a SHA-256 hash of the photograph's FILE NAME (not its
 * folder, not its pixels) so that rows can be matched to photographs
 * without writing the file name itself.
 */
public final class FileHash {

    private FileHash() {
    }

    /** @return lowercase hexadecimal SHA-256 of the UTF-8 bytes of {@code s} */
    public static String sha256Hex(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    /** @return hash of the file name only, or "" if {@code f} is null */
    public static String fileNameHash(File f) {
        return f == null ? "" : sha256Hex(f.getName());
    }
}
