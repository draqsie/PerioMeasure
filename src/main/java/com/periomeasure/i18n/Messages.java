package com.periomeasure.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * All UI texts, in every language, live in ONE file:
 * {@code com/periomeasure/i18n/messages.properties}.
 *
 * <p>File format (deliberately simpler than java.util.Properties):
 * <ul>
 *   <li>UTF-8, one entry per line: {@code <lang>.<key>=<text>}</li>
 *   <li>blank lines and lines starting with {@code #} are ignored</li>
 *   <li>the two characters {@code \n} in a text become a line break</li>
 *   <li>placeholders are {@code {0}}, {@code {1}}, ...; apostrophes need no
 *       escaping (unlike java.text.MessageFormat, where a Turkish
 *       {@code Fiji'de} would silently swallow text)</li>
 *   <li>a duplicated key is an error, not a silent overwrite</li>
 * </ul>
 * Numbers must be formatted by the caller with an explicit Locale before
 * being passed as arguments.
 */
public final class Messages {

    static final String RESOURCE = "messages.properties";

    private static final Map<String, String> TABLE = loadResource();

    private Messages() {
    }

    /**
     * @param lang language
     * @param msg  text key
     * @param args values for {@code {0}}, {@code {1}}, ... (via String.valueOf)
     * @return the formatted text
     */
    public static String get(Language lang, Msg msg, Object... args) {
        String pattern = TABLE.get(lang.code() + "." + msg.key());
        if (pattern == null) {
            // Unreachable when MessagesTest passes; never show a blank to the user.
            return "!" + lang.code() + "." + msg.key() + "!";
        }
        return format(pattern, args);
    }

    /** Replaces {@code {i}} with {@code String.valueOf(args[i])}; nothing else is special. */
    static String format(String pattern, Object... args) {
        String out = pattern;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                out = out.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }
        return out;
    }

    /** @return unmodifiable view of all entries, keyed {@code <lang>.<key>} (for tests). */
    static Map<String, String> table() {
        return TABLE;
    }

    private static Map<String, String> loadResource() {
        try (InputStream in = Messages.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE + " (broken build)");
            }
            return parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, String> parse(BufferedReader reader) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        String line;
        int lineNo = 0;
        while ((line = reader.readLine()) != null) {
            lineNo++;
            if (lineNo == 1 && !line.isEmpty() && line.charAt(0) == '﻿') {
                line = line.substring(1); // tolerate a UTF-8 BOM
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                throw new IllegalStateException(RESOURCE + " line " + lineNo + ": expected <lang>.<key>=<text>");
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim().replace("\\n", "\n");
            if (map.containsKey(key)) {
                throw new IllegalStateException(RESOURCE + " line " + lineNo + ": duplicate key " + key);
            }
            map.put(key, value);
        }
        return Collections.unmodifiableMap(map);
    }
}
