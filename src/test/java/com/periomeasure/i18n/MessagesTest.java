package com.periomeasure.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class MessagesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)}");

    @Test
    void defaultLanguageIsEnglish() {
        assertEquals(Language.EN, Language.DEFAULT);
        assertEquals(Language.EN, Language.fromCode(null));
        assertEquals(Language.EN, Language.fromCode("xx"));
        assertEquals(Language.EN, Language.fromCode(" EN "));
    }

    @Test
    void everyKeyHasNonBlankTextInEveryLanguage() {
        for (Msg m : Msg.values()) {
            for (Language l : Language.values()) {
                String text = Messages.table().get(l.code() + "." + m.key());
                assertTrue(text != null && !text.trim().isEmpty(), "missing text: " + l.code() + "." + m.key());
            }
        }
    }

    @Test
    void fileHasNoUnknownKeysOrLanguages() {
        Set<String> expected = new HashSet<>();
        for (Msg m : Msg.values()) {
            for (Language l : Language.values()) {
                expected.add(l.code() + "." + m.key());
            }
        }
        Set<String> unknown = new TreeSet<>(Messages.table().keySet());
        unknown.removeAll(expected);
        assertTrue(unknown.isEmpty(), "keys in messages.properties without a Msg constant: " + unknown);
    }

    @Test
    void placeholdersMatchAcrossLanguages() {
        for (Msg m : Msg.values()) {
            Set<String> reference = null;
            for (Language l : Language.values()) {
                Set<String> found = placeholders(Messages.table().get(l.code() + "." + m.key()));
                if (reference == null) {
                    reference = found;
                } else {
                    assertEquals(reference, found, "placeholder mismatch for " + m.key());
                }
            }
        }
    }

    @Test
    void turkishCharactersSurviveUtf8Loading() {
        String title = Messages.get(Language.TR, Msg.ABOUT_TITLE);
        assertEquals("PerioMeasure Hakkında", title);
        assertTrue(Messages.get(Language.TR, Msg.ABOUT_BODY, "1.0").contains("tıbbi cihaz değildir"));
    }

    @Test
    void formatReplacesPlaceholdersAndKeepsApostrophes() {
        assertEquals("Fiji'de 3 nokta, 'x'", Messages.format("Fiji'de {0} nokta, '{1}'", 3, "x"));
        assertEquals("{0}", Messages.format("{0}"));
    }

    @Test
    void lineBreakEscapeBecomesNewline() {
        assertTrue(Messages.get(Language.EN, Msg.ABOUT_BODY, "1.0").contains("\n"));
        assertFalse(Messages.get(Language.EN, Msg.ABOUT_BODY, "1.0").contains("\\n"));
    }

    @Test
    void duplicateKeyIsRejected() {
        String src = "tr.a=1\ntr.a=2\n";
        assertThrows(IllegalStateException.class, () -> Messages.parse(new BufferedReader(new StringReader(src))));
    }

    @Test
    void malformedLineIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> Messages.parse(new BufferedReader(new StringReader("no equals sign\n"))));
    }

    @Test
    void commentsBlankLinesAndBomAreIgnored() throws Exception {
        Map<String, String> m = Messages.parse(new BufferedReader(new StringReader("﻿# c\n\n tr.k = v = w \n")));
        assertEquals(1, m.size());
        assertEquals("v = w", m.get("tr.k"));
    }

    private static Set<String> placeholders(String text) {
        Set<String> out = new TreeSet<>();
        Matcher mm = PLACEHOLDER.matcher(text);
        while (mm.find()) {
            out.add(mm.group(1));
        }
        return out;
    }
}
