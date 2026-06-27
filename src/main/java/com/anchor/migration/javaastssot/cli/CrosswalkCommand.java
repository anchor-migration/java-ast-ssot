package com.anchor.migration.javaastssot.cli;

import com.anchor.migration.javaastssot.crosswalk.CrosswalkLinkEngine;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkLinkResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "crosswalk", description = "Link code SSOT profile bindings to schema SSOT (ADR-004)")
public final class CrosswalkCommand implements Callable<Integer> {

    @Option(names = {"--code-db"}, required = true, description = "SQLite Java AST SSOT from export")
    Path codeDb;

    @Option(names = {"--schema-db"}, required = true, description = "SQLite schema SSOT from db-metadata")
    Path schemaDb;

    @Option(names = {"--out", "-o"}, required = true, description = "Output linked SQLite file")
    Path out;

    @Option(
            names = {"--db-schema"},
            required = true,
            description = "Database schema/catalog name for stable IDs (e.g. dukesbank)")
    String dbSchema;

    @Option(names = {"--code-run-id"}, description = "Code export_run id (default: latest)")
    Integer codeRunId;

    @Option(names = {"--schema-run-id"}, description = "Schema export_run id (default: latest)")
    Integer schemaRunId;

    @Option(
            names = {"--fail-on-error"},
            description = "Exit non-zero when crosswalk validation reports errors (default: true)")
    boolean failOnError = true;

    @Override
    public Integer call() throws Exception {
        CrosswalkLinkResult result =
                new CrosswalkLinkEngine()
                        .link(codeDb, schemaDb, out, dbSchema, codeRunId, schemaRunId);

        System.out.printf("Crosswalk run:    %d%n", result.crosswalkRunId());
        System.out.printf("DB schema:        %s%n", dbSchema);
        System.out.printf(
                "Profiles linked:  %s%n",
                result.profilesLinked().isEmpty() ? "(none)" : String.join(", ", result.profilesLinked()));
        System.out.printf("Links written:    %d%n", result.linkCount());
        System.out.printf("Issues:           %d (%d errors)%n", result.issueCount(), result.errorCount());
        System.out.printf("Written to       %s%n", out);

        if (failOnError && result.errorCount() > 0) {
            return 1;
        }
        return 0;
    }
}
