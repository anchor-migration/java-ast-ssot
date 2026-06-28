package com.anchor.migration.javaastssot.listusage;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ListUsageAnalyzer {

    private final JavaParser parser;
    private final ListUsageClassifier classifier = new ListUsageClassifier();

    public ListUsageAnalyzer() {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_1_4);
        this.parser = new JavaParser(config);
    }

    public ListUsageReport analyze(Path sourceRoot, List<String> pathFilters) throws IOException {
        Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        List<ListUsageRecord> records = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(normalizedRoot)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> matchesFilter(normalizedRoot.relativize(path), pathFilters))
                    .forEach(path -> records.addAll(classifyPath(path, normalizedRoot)));
        }

        return new ListUsageReport(normalizedRoot.toString(), List.copyOf(records));
    }

    public List<ListUsageRecord> classifyPath(Path file, Path sourceRoot) {
        try {
            String relative = sourceRoot.relativize(file).toString().replace('\\', '/');
            String source = Files.readString(file);
            CompilationUnit cu =
                    parser.parse(source).getResult().orElseThrow(() -> new IllegalStateException("Parse failed: " + file));
            return classifier.classifyFile(cu, relative);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + file + ": " + ex.getMessage(), ex);
        }
    }

    static boolean matchesFilter(Path relative, List<String> pathFilters) {
        if (pathFilters == null || pathFilters.isEmpty()) {
            return true;
        }
        String normalized = relative.toString().replace('\\', '/');
        for (String filter : pathFilters) {
            String needle = filter.replace('\\', '/');
            if (normalized.equals(needle) || normalized.endsWith("/" + needle) || normalized.endsWith(needle)) {
                return true;
            }
        }
        return false;
    }
}
