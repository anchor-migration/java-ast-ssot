package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.listusage.ListUsageAnalyzer;
import com.anchor.migration.javaastssot.listusage.ListUsageRecord;
import com.anchor.migration.javaastssot.listusage.UsageClass;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ListUsageClassifierTest {

    private static final Path FIXTURE_ROOT =
            Path.of("src/test/resources/list-usage").toAbsolutePath().normalize();

    private final ListUsageAnalyzer analyzer = new ListUsageAnalyzer();

    @Test
    void homogeneousAddsClassifyAsHomogeneous() {
        ListUsageRecord record =
                requireSite("demo/homogeneous/HomogeneousFixture.java", "detailsList");
        assertEquals(UsageClass.homogeneous, record.usageClass());
        assertTrue(record.elementTypes().stream().anyMatch(t -> t.contains("AccountDetails")));
    }

    @Test
    void mixedAddsClassifyAsTuple() {
        ListUsageRecord record = requireSite("demo/tuple/TupleFixture.java", "out");
        assertEquals(UsageClass.tuple, record.usageClass());
        assertTrue(record.elementTypes().contains("String"));
        assertTrue(record.elementTypes().contains("Integer"));
    }

    @Test
    void mixedCastGetsClassifyAsTuple() {
        ListUsageRecord record = requireSite("demo/cast/CastTupleFixture.java", "row");
        assertEquals(UsageClass.tuple, record.usageClass());
        assertTrue(record.elementTypes().contains("String"));
        assertTrue(record.elementTypes().contains("Integer"));
    }

    @Test
    void unusedSitesAreNotReported() {
        var report = analyze("demo/unknown/UnknownFixture.java");
        assertTrue(report.stream().noneMatch(r -> "unused".equals(r.variableName())));
        assertTrue(report.stream().noneMatch(r -> "idle".equals(r.variableName())));
    }

    @Test
    void pathFilterLimitsScan() throws Exception {
        var report =
                analyzer.analyze(
                        FIXTURE_ROOT,
                        List.of("demo/homogeneous/HomogeneousFixture.java"));
        assertEquals(1, report.records().size());
        assertTrue(report.records().get(0).relativePath().endsWith("HomogeneousFixture.java"));
    }

    @Test
    void dukesBankCopyAccountsPatternIsHomogeneousWhenPresent() {
        Path bankRoot =
                Path.of("C:/github/dukesbank/src/j2eetutorial14/examples/bank/src").normalize();
        Path bean =
                bankRoot.resolve("com/sun/ebank/ejb/account/AccountControllerBean.java");
        assumeTrue(Files.isRegularFile(bean), "Duke's Bank checkout not available");

        ListUsageRecord record =
                requireSiteOnDisk(
                        bankRoot,
                        "com/sun/ebank/ejb/account/AccountControllerBean.java",
                        "detailsList");
        assertEquals(UsageClass.homogeneous, record.usageClass());
    }

    private ListUsageRecord requireSite(String relativeFixture, String variableName) {
        return requireSiteOnDisk(FIXTURE_ROOT, relativeFixture, variableName);
    }

    private ListUsageRecord requireSiteOnDisk(Path root, String relativeFixture, String variableName) {
        List<ListUsageRecord> records = analyzeOnDisk(root, relativeFixture);
        Optional<ListUsageRecord> match =
                records.stream().filter(r -> variableName.equals(r.variableName())).findFirst();
        assertTrue(match.isPresent(), "No site for variable " + variableName + " in " + records);
        return match.get();
    }

    private List<ListUsageRecord> analyze(String relativeFixture) {
        return analyzeOnDisk(FIXTURE_ROOT, relativeFixture);
    }

    private List<ListUsageRecord> analyzeOnDisk(Path root, String relativeFixture) {
        Path file = root.resolve(relativeFixture);
        return analyzer.classifyPath(file, root);
    }
}
