package com.anchor.migration.codessot;

import com.anchor.migration.codessot.extract.CodeExtractor;
import com.anchor.migration.codessot.model.ExportSnapshot;
import com.anchor.migration.codessot.profile.ExportProfile;
import com.anchor.migration.codessot.profile.ProfileRegistry;
import com.anchor.migration.codessot.store.CodeSsotWriter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
        name = "java-ast-ssot",
        mixinStandardHelpOptions = true,
        subcommands = {ExportCommand.class, InfoCommand.class, ProfilesCommand.class})
public final class Cli implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exit = new CommandLine(new Cli()).execute(args);
        System.exit(exit);
    }
}

@Command(name = "export", description = "Export Java sources to SQLite Java AST SSOT (optional stack profiles)")
final class ExportCommand implements Callable<Integer> {

    @Option(
            names = {"--source-root", "-s"},
            required = true,
            description = "Root directory to scan")
    Path sourceRoot;

    @Option(
            names = {"--out", "-o"},
            required = true,
            description = "Output SQLite file path")
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
        CodeExtractor extractor = new CodeExtractor();
        ExportSnapshot snapshot = extractor.extract(sourceRoot, resolved);
        CodeSsotWriter writer = new CodeSsotWriter();
        int runId = writer.write(out, snapshot);

        System.out.printf(
                "Exported %d Java files, %d types, %d methods%n",
                snapshot.javaFileCount, snapshot.javaTypes.size(), snapshot.javaMethods.size());
        if (resolved.isEmpty()) {
            System.out.println("Profiles: (core only)");
        } else {
            System.out.printf("Profiles: %s%n", String.join(", ", resolved));
            for (String profileId : resolved) {
                ExportProfile profile = ProfileRegistry.require(profileId);
                ExportProfile.ProfileCounts counts = profile.counts(snapshot);
                System.out.printf(
                        "  %s: %d primary entities, %d crosswalk edges%n",
                        profileId, counts.primaryEntityCount(), counts.crosswalkEdgeCount());
            }
        }
        System.out.printf("Written to %s (export_run_id=%d)%n", out, runId);
        return 0;
    }
}

@Command(name = "info", description = "Show summary of exported Java AST SSOT")
final class InfoCommand implements Callable<Integer> {

    @Option(
            names = {"--db", "-d"},
            required = true,
            description = "SQLite Java AST SSOT file")
    Path db;

    @Override
    public Integer call() throws Exception {
        CodeSsotWriter writer = new CodeSsotWriter();
        CodeSsotWriter.ExportInfo info = writer.readInfo(db);
        System.out.printf("Export run:     %d%n", info.exportRunId());
        System.out.printf("Source root:    %s%n", info.sourceRoot());
        System.out.printf("Exported at:    %s%n", info.exportedAt());
        System.out.printf(
                "Profiles:       %s%n", info.coreOnly() ? "(core only)" : info.profiles());
        System.out.printf("Java types:     %d%n", info.javaTypeCount());
        System.out.printf("Java methods:   %d%n", info.javaMethodCount());
        if (!info.coreOnly()) {
            System.out.printf("EJB beans:      %d%n", info.ejbBeanCount());
            System.out.printf("Crosswalk edges:%d%n", info.crosswalkEdgeCount());
        }
        return 0;
    }
}

@Command(name = "profiles", description = "List available stack profiles")
final class ProfilesCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        for (String id : ProfileRegistry.knownProfileIds()) {
            ExportProfile profile = ProfileRegistry.require(id);
            System.out.printf("%s%n", id);
            System.out.printf("  schema: %s%n", profile.schemaResource());
        }
        return 0;
    }
}
