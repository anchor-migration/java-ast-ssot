package com.anchor.migration.javaastssot.cli;

import com.anchor.migration.javaastssot.listusage.ListUsageAnalyzer;
import com.anchor.migration.javaastssot.listusage.ListUsageJsonWriter;
import com.anchor.migration.javaastssot.listusage.ListUsageRecord;
import com.anchor.migration.javaastssot.listusage.ListUsageReport;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "classify-lists",
        description = "On-demand raw collection usage classifier (ADR-008 M2). Ephemeral JSON — no SSOT cache.")
public final class ClassifyListsCommand implements Callable<Integer> {

    @Option(names = {"--source-root", "-s"}, required = true, description = "Root directory to scan")
    Path sourceRoot;

    @Option(
            names = {"--paths"},
            description = "Relative path filters (repeatable). If omitted, scans all .java files under source root.")
    List<String> paths = new ArrayList<>();

    @Option(names = {"--out", "-o"}, description = "Optional output JSON file. Prints to stdout if omitted.")
    Path out;

    @Override
    public Integer call() throws Exception {
        ListUsageReport report = new ListUsageAnalyzer().analyze(sourceRoot, paths);
        String json = ListUsageJsonWriter.toJson(report);

        if (out != null) {
            Files.writeString(out, json);
            System.out.printf("Wrote %d record(s) to %s%n", report.records().size(), out);
        } else {
            System.out.print(json);
        }

        printSummary(report);
        return 0;
    }

    private static void printSummary(ListUsageReport report) {
        long homogeneous =
                report.records().stream().filter(r -> r.usageClass() == com.anchor.migration.javaastssot.listusage.UsageClass.homogeneous).count();
        long tuple =
                report.records().stream().filter(r -> r.usageClass() == com.anchor.migration.javaastssot.listusage.UsageClass.tuple).count();
        long unknown =
                report.records().stream().filter(r -> r.usageClass() == com.anchor.migration.javaastssot.listusage.UsageClass.unknown).count();
        System.err.printf(
                "Summary: %d site(s) — homogeneous=%d tuple=%d unknown=%d%n",
                report.records().size(), homogeneous, tuple, unknown);
        for (ListUsageRecord record : report.records()) {
            System.err.printf(
                    "  %s %s [%s] types=%s%n",
                    record.relativePath(),
                    record.variableName(),
                    record.usageClass(),
                    record.elementTypes());
        }
    }
}
