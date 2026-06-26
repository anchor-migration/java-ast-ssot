package com.anchor.migration.codessot;

import com.anchor.migration.codessot.extract.CodeExtractor;
import com.anchor.migration.codessot.model.ExportSnapshot;
import com.anchor.migration.codessot.store.CodeSsotWriter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "java-ast-ssot",
        mixinStandardHelpOptions = true,
        subcommands = {ExportCommand.class, InfoCommand.class})
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

@Command(name = "export", description = "Export Java sources and EJB descriptors to SQLite Java AST SSOT")
final class ExportCommand implements Callable<Integer> {

    @Option(
            names = {"--source-root", "-s"},
            required = true,
            description = "Root directory to scan (e.g. Duke's Bank bank module)")
    Path sourceRoot;

    @Option(
            names = {"--out", "-o"},
            required = true,
            description = "Output SQLite file path")
    Path out;

    @Override
    public Integer call() throws Exception {
        CodeExtractor extractor = new CodeExtractor();
        ExportSnapshot snapshot = extractor.extract(sourceRoot);
        CodeSsotWriter writer = new CodeSsotWriter();
        int runId = writer.write(out, snapshot);
        System.out.printf(
                "Exported %d Java files, %d types, %d EJB beans, %d crosswalk edges%n",
                snapshot.javaFileCount,
                snapshot.javaTypes.size(),
                snapshot.ejbBeans.size(),
                snapshot.crosswalkEdges.size());
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
        System.out.printf("Java types:     %d%n", info.javaTypeCount());
        System.out.printf("Java methods:   %d%n", info.javaMethodCount());
        System.out.printf("EJB beans:      %d%n", info.ejbBeanCount());
        System.out.printf("Crosswalk edges:%d%n", info.crosswalkEdgeCount());
        return 0;
    }
}
