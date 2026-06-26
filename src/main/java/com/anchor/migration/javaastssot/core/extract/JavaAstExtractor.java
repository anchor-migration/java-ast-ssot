package com.anchor.migration.javaastssot.core.extract;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.profile.ExportProfile;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public final class JavaAstExtractor {

    private final JavaSourceExtractor javaExtractor = new JavaSourceExtractor();

    public ExportSnapshot extract(Path sourceRoot, Set<String> profileIds) throws IOException {
        Path normalizedRoot = sourceRoot.toAbsolutePath().normalize();
        ExportSnapshot snapshot = new ExportSnapshot();
        snapshot.sourceRoot = normalizedRoot.toString();

        for (String profileId : profileIds) {
            ExportProfile profile = ProfileRegistry.require(profileId);
            snapshot.attachProfile(profileId, profile.newSnapshot());
        }

        try (Stream<Path> walk = Files.walk(normalizedRoot)) {
            walk.filter(Files::isRegularFile)
                    .forEach(path -> processFile(path, normalizedRoot, snapshot, profileIds));
        }

        return snapshot;
    }

    private void processFile(Path file, Path sourceRoot, ExportSnapshot snapshot, Set<String> profileIds) {
        String name = file.getFileName().toString();
        try {
            if (name.endsWith(".java")) {
                javaExtractor.parseFile(file, sourceRoot, snapshot);
                return;
            }
            for (String profileId : profileIds) {
                ExportProfile profile = ProfileRegistry.require(profileId);
                if (profile.handlesFileName(name)) {
                    profile.processFile(file, sourceRoot, snapshot);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process " + file + ": " + ex.getMessage(), ex);
        }
    }
}
