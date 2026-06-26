package com.anchor.migration.codessot.extract;

import com.anchor.migration.codessot.model.ExportSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class CodeExtractor {

    private final JavaSourceExtractor javaExtractor = new JavaSourceExtractor();
    private final EjbDescriptorExtractor ejbExtractor = new EjbDescriptorExtractor();

    public ExportSnapshot extract(Path sourceRoot) throws IOException {
        Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        ExportSnapshot snapshot = new ExportSnapshot();
        snapshot.sourceRoot = normalizedRoot.toString();

        try (Stream<Path> walk = Files.walk(normalizedRoot)) {
            walk.filter(Files::isRegularFile).forEach(path -> processFile(path, normalizedRoot, snapshot));
        }

        return snapshot;
    }

    private void processFile(Path file, Path sourceRoot, ExportSnapshot snapshot) {
        String name = file.getFileName().toString();
        try {
            if (name.endsWith(".java")) {
                javaExtractor.parseFile(file, sourceRoot, snapshot);
            } else if (name.equals("ejb-jar.xml")) {
                ejbExtractor.parseEjbJar(file, sourceRoot, snapshot);
            } else if (name.equals("jbosscmp-jdbc.xml")) {
                ejbExtractor.parseJbossCmpJdbc(file, sourceRoot, snapshot);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process " + file + ": " + ex.getMessage(), ex);
        }
    }
}
