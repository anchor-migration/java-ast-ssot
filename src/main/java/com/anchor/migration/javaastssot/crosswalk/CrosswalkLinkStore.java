package com.anchor.migration.javaastssot.crosswalk;

import com.anchor.migration.javaastssot.Version;
import com.anchor.migration.javaastssot.crosswalk.model.CodeSchemaLinkRecord;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkIssue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class CrosswalkLinkStore {

    public int write(
            Path outDb,
            Path codeDb,
            Path schemaDb,
            int codeExportRunId,
            int schemaExportRunId,
            String dbSchema,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException, IOException {
        Files.createDirectories(outDb.getParent() != null ? outDb.getParent() : Path.of("."));
        if (Files.exists(outDb)) {
            Files.delete(outDb);
        }
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + outDb.toAbsolutePath())) {
            applyCrosswalkSchema(conn);
            conn.setAutoCommit(false);
            int crosswalkRunId =
                    insertCrosswalkRun(
                            conn,
                            codeDb,
                            schemaDb,
                            codeExportRunId,
                            schemaExportRunId,
                            dbSchema);
            insertLinks(conn, crosswalkRunId, links);
            insertIssues(conn, crosswalkRunId, issues);
            conn.commit();
            return crosswalkRunId;
        }
    }

    public CrosswalkSummary readSummary(Path linkedDb) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + linkedDb.toAbsolutePath());
                Statement st = conn.createStatement()) {
            ResultSet run =
                    st.executeQuery(
                            """
                            SELECT id, db_schema, code_export_run_id, schema_export_run_id, linked_at
                            FROM crosswalk_run ORDER BY id DESC LIMIT 1
                            """);
            if (!run.next()) {
                throw new IllegalStateException("No crosswalk runs in " + linkedDb);
            }
            int runId = run.getInt("id");
            return new CrosswalkSummary(
                    runId,
                    run.getString("db_schema"),
                    run.getInt("code_export_run_id"),
                    run.getInt("schema_export_run_id"),
                    run.getString("linked_at"),
                    count(conn, "code_schema_link", runId),
                    count(conn, "crosswalk_issue", runId),
                    countIssues(conn, runId, "error"));
        }
    }

    private int count(Connection conn, String table, int runId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE crosswalk_run_id = ?")) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int countIssues(Connection conn, int runId, String severity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM crosswalk_issue WHERE crosswalk_run_id = ? AND severity = ?")) {
            ps.setInt(1, runId);
            ps.setString(2, severity);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void applyCrosswalkSchema(Connection conn) throws IOException, SQLException {
        String ddl;
        try (var in = CrosswalkLinkStore.class.getResourceAsStream("/schema/crosswalk/v1.sql")) {
            if (in == null) {
                throw new IOException("Crosswalk schema resource not found");
            }
            ddl = new String(in.readAllBytes());
        }
        try (Statement st = conn.createStatement()) {
            for (String stmt : ddl.split(";")) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty()) {
                    st.execute(trimmed);
                }
            }
        }
    }

    private int insertCrosswalkRun(
            Connection conn,
            Path codeDb,
            Path schemaDb,
            int codeExportRunId,
            int schemaExportRunId,
            String dbSchema)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO crosswalk_run (tool_version, code_db_path, schema_db_path,
                    code_export_run_id, schema_export_run_id, db_schema)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, Version.TOOL);
            ps.setString(2, codeDb.toAbsolutePath().toString());
            ps.setString(3, schemaDb.toAbsolutePath().toString());
            ps.setInt(4, codeExportRunId);
            ps.setInt(5, schemaExportRunId);
            ps.setString(6, dbSchema);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void insertLinks(Connection conn, int crosswalkRunId, List<CodeSchemaLinkRecord> links)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT OR IGNORE INTO code_schema_link
                    (crosswalk_run_id, edge_kind, source_stable_id, target_stable_id,
                     mapping_role, profile_id, binding_source, evidence_ref, confidence)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (CodeSchemaLinkRecord link : links) {
                ps.setInt(1, crosswalkRunId);
                ps.setString(2, link.edgeKind());
                ps.setString(3, link.sourceStableId());
                ps.setString(4, link.targetStableId());
                ps.setString(5, link.mappingRole());
                ps.setString(6, link.profileId());
                ps.setString(7, link.bindingSource());
                ps.setString(8, link.evidenceRef());
                ps.setString(9, link.confidence());
                ps.executeUpdate();
            }
        }
    }

    private void insertIssues(Connection conn, int crosswalkRunId, List<CrosswalkIssue> issues)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO crosswalk_issue (crosswalk_run_id, severity, issue_code, message, context_ref)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (CrosswalkIssue issue : issues) {
                ps.setInt(1, crosswalkRunId);
                ps.setString(2, issue.severity());
                ps.setString(3, issue.issueCode());
                ps.setString(4, issue.message());
                ps.setString(5, issue.contextRef());
                ps.executeUpdate();
            }
        }
    }

    public record CrosswalkSummary(
            int crosswalkRunId,
            String dbSchema,
            int codeExportRunId,
            int schemaExportRunId,
            String linkedAt,
            int linkCount,
            int issueCount,
            int errorCount) {}
}
