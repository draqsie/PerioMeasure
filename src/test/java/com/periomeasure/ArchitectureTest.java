package com.periomeasure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Enforces the architecture rule: the calculation core is pure Java. */
class ArchitectureTest {

    private static List<Path> sources(String pkgDir) throws IOException {
        Path dir = Paths.get("src/main/java/com/periomeasure", pkgDir);
        assertTrue(Files.isDirectory(dir), "missing " + dir.toAbsolutePath());
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    private static List<String> offenders(List<Path> files, String... forbiddenImportPrefixes) throws IOException {
        List<String> bad = new ArrayList<>();
        for (Path f : files) {
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String t = line.trim();
                for (String prefix : forbiddenImportPrefixes) {
                    if (t.startsWith("import " + prefix) || t.startsWith("import static " + prefix)) {
                        bad.add(f.getFileName() + ": " + t);
                    }
                }
            }
        }
        return bad;
    }

    @Test
    void coreDependsNeitherOnImageJNorOnAwt() throws IOException {
        List<String> bad = offenders(sources("core"), "ij.", "java.awt", "javax.");
        assertTrue(bad.isEmpty(), "Forbidden imports in core: " + bad);
    }

    @Test
    void sessionLogicIsPureJava() throws IOException {
        List<String> bad = offenders(sources("session"), "ij.", "java.awt", "javax.");
        assertTrue(bad.isEmpty(), "Forbidden imports in session: " + bad);
    }

    @Test
    void ioAndI18nDoNotDependOnImageJ() throws IOException {
        List<String> bad = offenders(sources("io"), "ij.");
        bad.addAll(offenders(sources("i18n"), "ij."));
        assertTrue(bad.isEmpty(), "Forbidden imports: " + bad);
    }

    @Test
    void syntheticGeneratorDoesNotDependOnImageJ() throws IOException {
        List<String> bad = offenders(sources("synthetic"), "ij.");
        assertTrue(bad.isEmpty(), "Forbidden imports in synthetic: " + bad);
    }
}
