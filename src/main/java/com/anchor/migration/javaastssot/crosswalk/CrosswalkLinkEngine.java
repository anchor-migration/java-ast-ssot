package com.anchor.migration.javaastssot.crosswalk;

import com.anchor.migration.javaastssot.crosswalk.contributor.CrosswalkContributor;
import com.anchor.migration.javaastssot.crosswalk.contributor.CrosswalkContributorRegistry;
import com.anchor.migration.javaastssot.crosswalk.model.CodeSchemaLinkRecord;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkContribution;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkIssue;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkLinkResult;
import com.anchor.migration.javaastssot.crosswalk.schema.SchemaSsotReader;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CrosswalkLinkEngine {

    public CrosswalkLinkResult link(
            Path codeDb,
            Path schemaDb,
            Path outDb,
            String dbSchema,
            Integer codeExportRunId,
            Integer schemaExportRunId)
            throws Exception {
        try (Connection codeConn = open(codeDb);
                Connection schemaConn = open(schemaDb)) {
            int codeRunId = resolveExportRunId(codeConn, "export_run", codeExportRunId);
            int schemaRunId = resolveExportRunId(schemaConn, "export_run", schemaExportRunId);
            List<String> profiles = readCodeProfiles(codeConn, codeRunId);
            SchemaSsotReader schemaReader = SchemaSsotReader.load(schemaConn, schemaRunId);
            CrosswalkContext context =
                    new CrosswalkContext(codeConn, schemaConn, codeRunId, schemaRunId, dbSchema, schemaReader);

            List<CodeSchemaLinkRecord> allLinks = new ArrayList<>();
            List<CrosswalkIssue> allIssues = new ArrayList<>();
            Set<String> profilesLinked = new LinkedHashSet<>();

            for (CrosswalkContributor contributor : CrosswalkContributorRegistry.forProfiles(profiles)) {
                CrosswalkContribution contribution = contributor.contribute(context);
                allLinks.addAll(contribution.links());
                allIssues.addAll(contribution.issues());
                profilesLinked.add(contributor.profileId());
            }

            int errorCount = (int) allIssues.stream().filter(i -> "error".equals(i.severity())).count();
            CrosswalkLinkStore store = new CrosswalkLinkStore();
            int crosswalkRunId =
                    store.write(
                            outDb,
                            codeDb,
                            schemaDb,
                            codeRunId,
                            schemaRunId,
                            dbSchema,
                            allLinks,
                            allIssues);

            return new CrosswalkLinkResult(
                    crosswalkRunId,
                    allLinks.size(),
                    allIssues.size(),
                    errorCount,
                    List.copyOf(profilesLinked));
        }
    }

    private static Connection open(Path dbPath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    private static int resolveExportRunId(Connection conn, String table, Integer explicitRunId)
            throws SQLException {
        if (explicitRunId != null) {
            verifyExportRunExists(conn, table, explicitRunId);
            return explicitRunId;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM " + table + " ORDER BY id DESC LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("No export runs in database");
                }
                return rs.getInt(1);
            }
        }
    }

    private static void verifyExportRunExists(Connection conn, String table, int runId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM " + table + " WHERE id = ?")) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("export_run id not found: " + runId);
                }
            }
        }
    }

    private static List<String> readCodeProfiles(Connection codeConn, int codeRunId) throws SQLException {
        try (PreparedStatement ps = codeConn.prepareStatement("SELECT profiles FROM export_run WHERE id = ?")) {
            ps.setInt(1, codeRunId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Code export run not found: " + codeRunId);
                }
                String csv = rs.getString(1);
                if (csv == null || csv.isBlank()) {
                    return List.of();
                }
                return List.of(csv.split(","));
            }
        }
    }
}
