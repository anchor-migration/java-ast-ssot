package com.anchor.migration.javaastssot.cli;

import com.anchor.migration.javaastssot.core.extract.JavaAstExtractor;
import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import com.anchor.migration.javaastssot.profile.ExportProfile;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "export", description = "Export Java AST SSOT (core + optional stack profiles)")
public final class ExportCommand implements Callable<Integer> {

    @Option(names = {"--source-root", "-s"}, required = true, description = "Root directory to scan")
    Path sourceRoot;

    @Option(names = {"--out", "-o"}, required = true, description = "Output SQLite file path")
    Path out;

    @Option(
            names = {"--profile", "-p"},
            description = "Stack profile to enable (repeatable). Example: javaee-ejb2-jboss")
    List<String> profiles = new ArrayList<>();

    @Option(
            names = {"--auto-detect-profiles"},
            description = "Also enable profiles detected from descriptor files (default: false)")
    boolean autoDetectProfiles;

    @Override
    public Integer call() throws Exception {
        Set<String> resolved = ProfileRegistry.resolve(profiles, sourceRoot, autoDetectProfiles);
        ExportSnapshot snapshot = new JavaAstExtractor().extract(sourceRoot, resolved);
        new JavaAstSsotStore().write(out, snapshot);

        System.out.printf(
                "Exported %d Java files, %d types, %d methods, %d comments%n",
                snapshot.javaFileCount,
                snapshot.javaTypes.size(),
                snapshot.javaMethods.size(),
                snapshot.sourceComments.size());
        if (resolved.isEmpty()) {
            System.out.println("Profiles: (core only)");
        } else {
            System.out.printf("Profiles: %s%n", String.join(", ", resolved));
            for (String profileId : resolved) {
                ExportProfile profile = ProfileRegistry.require(profileId);
                ExportProfile.ProfileStats stats = profile.stats(snapshot);
                System.out.printf(
                        "  %s: %d entities, %d crosswalk edges%n",
                        profileId, stats.entityCount(), stats.crosswalkCount());
            }
        }
        System.out.printf("Written to %s%n", out);
        return 0;
    }
}
