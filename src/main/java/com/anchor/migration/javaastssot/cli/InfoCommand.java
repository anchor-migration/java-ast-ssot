package com.anchor.migration.javaastssot.cli;

import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import com.anchor.migration.javaastssot.profile.ExportProfile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "info", description = "Show summary of exported Java AST SSOT")
public final class InfoCommand implements Callable<Integer> {

    @Option(names = {"--db", "-d"}, required = true, description = "SQLite Java AST SSOT file")
    Path db;

    @Override
    public Integer call() throws Exception {
        JavaAstSsotStore.ExportSummary summary = new JavaAstSsotStore().readSummary(db);
        System.out.printf("Export run:     %d%n", summary.exportRunId());
        System.out.printf("Source root:    %s%n", summary.sourceRoot());
        System.out.printf("Exported at:    %s%n", summary.exportedAt());
        System.out.printf(
                "Profiles:       %s%n",
                summary.profiles().isEmpty() ? "(core only)" : String.join(", ", summary.profiles()));
        System.out.printf("Java types:     %d%n", summary.javaTypeCount());
        System.out.printf("Java methods:   %d%n", summary.javaMethodCount());
        for (var entry : summary.profileStats().entrySet()) {
            ExportProfile.ProfileStats stats = entry.getValue();
            System.out.printf(
                    "  %s entities:  %d%n  %s crosswalk: %d%n",
                    entry.getKey(), stats.entityCount(), entry.getKey(), stats.crosswalkCount());
        }
        return 0;
    }
}
