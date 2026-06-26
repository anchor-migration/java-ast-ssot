package com.anchor.migration.codessot.store;

import com.anchor.migration.codessot.Version;
import com.anchor.migration.codessot.model.*;
import com.anchor.migration.codessot.profile.ExportProfile;
import com.anchor.migration.codessot.profile.ProfileRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class CodeSsotWriter {

    public void initDb(Path dbPath, Iterable<String> profileIds) throws IOException, SQLException {
        Files.createDirectories(dbPath.getParent() != null ? dbPath.getParent() : Path.of("."));
        try (Connection conn = connect(dbPath)) {
            SchemaRunner.applyCoreSchema(conn);
            SchemaRunner.applyProfileSchemas(conn, profileIds);
        }
    }

    public int write(Path dbPath, ExportSnapshot snapshot) throws SQLException, IOException {
        initDb(dbPath, snapshot.enabledProfiles);
        try (Connection conn = connect(dbPath)) {
            conn.setAutoCommit(false);
            int exportRunId = insertExportRun(conn, snapshot);
            Map<String, Integer> sourceFileIds = insertSourceFiles(conn, exportRunId, snapshot);
            Map<String, Integer> typeIds = insertJavaTypes(conn, exportRunId, sourceFileIds, snapshot);
            insertJavaMethods(conn, exportRunId, typeIds, snapshot);
            insertJavaFields(conn, exportRunId, typeIds, snapshot);
            insertJavaImports(conn, exportRunId, sourceFileIds, snapshot);
            for (String profileId : snapshot.enabledProfiles) {
                ProfileRegistry.require(profileId).writeSql(conn, exportRunId, snapshot);
            }
            conn.commit();
            return exportRunId;
        }
    }

    public ExportInfo readInfo(Path dbPath) throws SQLException {
        try (Connection conn = connect(dbPath);
                Statement st = conn.createStatement()) {
            ResultSet run =
                    st.executeQuery(
                            "SELECT id, source_root, exported_at, profiles FROM export_run ORDER BY id DESC LIMIT 1");
            if (!run.next()) {
                throw new IllegalStateException("No export runs in " + dbPath);
            }
            int runId = run.getInt("id");
            String profiles = run.getString("profiles");
            if (profiles == null) {
                profiles = "";
            }
            return new ExportInfo(
                    runId,
                    run.getString("source_root"),
                    run.getString("exported_at"),
                    profiles,
                    count(conn, "java_type", runId),
                    count(conn, "java_method", runId),
                    tableExists(conn, "ejb_bean") ? count(conn, "ejb_bean", runId) : 0,
                    tableExists(conn, "profile_crosswalk_edge")
                            ? count(conn, "profile_crosswalk_edge", runId)
                            : legacyCrosswalkCount(conn, runId));
        }
    }

    private int legacyCrosswalkCount(Connection conn, int runId) throws SQLException {
        if (!tableExists(conn, "crosswalk_edge")) {
            return 0;
        }
        return count(conn, "crosswalk_edge", runId);
    }

    private boolean tableExists(Connection conn, String table) throws SQLException {
        try (ResultSet rs =
                conn.getMetaData().getTables(null, null, table, new String[] {"TABLE"})) {
            return rs.next();
        }
    }

    private int count(Connection conn, String table, int runId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE export_run_id = ?")) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Connection connect(Path dbPath) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }

    private int insertExportRun(Connection conn, ExportSnapshot snapshot) throws SQLException {
        String profiles = String.join(",", snapshot.enabledProfiles);
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO export_run (source_root, tool_version, java_file_count, profiles)
                VALUES (?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, snapshot.sourceRoot);
            ps.setString(2, Version.TOOL);
            ps.setInt(3, snapshot.javaFileCount);
            ps.setString(4, profiles);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private Map<String, Integer> insertSourceFiles(
            Connection conn, int runId, ExportSnapshot snapshot) throws SQLException {
        Map<String, Integer> ids = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO source_file (export_run_id, relative_path, file_kind) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            for (SourceFileRecord file : snapshot.sourceFiles) {
                if (ids.containsKey(file.relativePath())) {
                    continue;
                }
                ps.setInt(1, runId);
                ps.setString(2, file.relativePath());
                ps.setString(3, file.fileKind());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    ids.put(file.relativePath(), keys.getInt(1));
                }
            }
        }
        return ids;
    }

    private Map<String, Integer> insertJavaTypes(
            Connection conn, int runId, Map<String, Integer> sourceFileIds, ExportSnapshot snapshot)
            throws SQLException {
        Map<String, Integer> ids = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO java_type (export_run_id, source_file_id, stable_id, package_name,
                    simple_name, kind, extends_type, implements_list)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                Statement.RETURN_GENERATED_KEYS)) {
            for (JavaTypeRecord type : snapshot.javaTypes) {
                ps.setInt(1, runId);
                ps.setInt(2, sourceFileIds.get(type.relativePath()));
                ps.setString(3, type.stableId());
                ps.setString(4, type.packageName());
                ps.setString(5, type.simpleName());
                ps.setString(6, type.kind());
                ps.setString(7, type.extendsType());
                ps.setString(8, type.implementsList());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    ids.put(type.stableId(), keys.getInt(1));
                }
            }
        }
        return ids;
    }

    private void insertJavaMethods(
            Connection conn, int runId, Map<String, Integer> typeIds, ExportSnapshot snapshot)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO java_method (export_run_id, type_id, stable_id, name, return_type, modifiers)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (JavaMethodRecord method : snapshot.javaMethods) {
                ps.setInt(1, runId);
                ps.setInt(2, typeIds.get(method.typeStableId()));
                ps.setString(3, method.stableId());
                ps.setString(4, method.name());
                ps.setString(5, method.returnType());
                ps.setString(6, method.modifiers());
                ps.executeUpdate();
            }
        }
    }

    private void insertJavaFields(
            Connection conn, int runId, Map<String, Integer> typeIds, ExportSnapshot snapshot)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO java_field (export_run_id, type_id, stable_id, name, field_type, modifiers)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (JavaFieldRecord field : snapshot.javaFields) {
                ps.setInt(1, runId);
                ps.setInt(2, typeIds.get(field.typeStableId()));
                ps.setString(3, field.stableId());
                ps.setString(4, field.name());
                ps.setString(5, field.fieldType());
                ps.setString(6, field.modifiers());
                ps.executeUpdate();
            }
        }
    }

    private void insertJavaImports(
            Connection conn, int runId, Map<String, Integer> sourceFileIds, ExportSnapshot snapshot)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO java_import (export_run_id, source_file_id, imported_name, is_static, is_wildcard)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (JavaImportRecord imp : snapshot.javaImports) {
                ps.setInt(1, runId);
                ps.setInt(2, sourceFileIds.get(imp.relativePath()));
                ps.setString(3, imp.importedName());
                ps.setInt(4, imp.isStatic() ? 1 : 0);
                ps.setInt(5, imp.isWildcard() ? 1 : 0);
                ps.executeUpdate();
            }
        }
    }

    public record ExportInfo(
            int exportRunId,
            String sourceRoot,
            String exportedAt,
            String profiles,
            int javaTypeCount,
            int javaMethodCount,
            int ejbBeanCount,
            int crosswalkEdgeCount) {

        public boolean coreOnly() {
            return profiles == null || profiles.isBlank();
        }
    }
}
