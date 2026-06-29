package com.anchor.migration.javaastssot.profile.jpa;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.profile.ExportProfile;
import com.anchor.migration.javaastssot.profile.ProfileSnapshot;
import com.anchor.migration.javaastssot.profile.jpa.model.JpaEntityRecord;
import com.anchor.migration.javaastssot.profile.jpa.model.JpaFieldRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class JpaProfile implements ExportProfile {

    public static final String ID = JpaIds.PROFILE_ID;

    private final JpaAnnotationExtractor extractor = new JpaAnnotationExtractor();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String schemaResource() {
        return "/schema/profiles/jpa/v1.sql";
    }

    @Override
    public boolean handlesFileName(String fileName) {
        return fileName.endsWith(".java");
    }

    @Override
    public boolean detect(Path sourceRoot) {
        try (var walk = Files.walk(sourceRoot)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .anyMatch(extractor::containsEntityAnnotation);
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public ProfileSnapshot newSnapshot() {
        return new JpaSnapshot();
    }

    @Override
    public void processFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        if (!file.getFileName().toString().endsWith(".java")) {
            return;
        }
        extractor.parseFile(file, sourceRoot, snapshot);
    }

    @Override
    public void writeSql(Connection conn, int exportRunId, ExportSnapshot snapshot) throws SQLException {
        JpaSnapshot data = snapshot.requireProfile(ID, JpaSnapshot.class);
        insertEntities(conn, exportRunId, data);
        insertFields(conn, exportRunId, data);
    }

    @Override
    public ProfileStats stats(ExportSnapshot snapshot) {
        if (!snapshot.hasProfile(ID)) {
            return new ProfileStats(0, 0);
        }
        JpaSnapshot data = snapshot.requireProfile(ID, JpaSnapshot.class);
        return new ProfileStats(data.entities.size(), data.fields.size());
    }

    @Override
    public ProfileStats readStats(Connection conn, int exportRunId) throws SQLException {
        return new ProfileStats(count(conn, "jpa_entity", exportRunId), count(conn, "jpa_field", exportRunId));
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

    private void insertEntities(Connection conn, int runId, JpaSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO jpa_entity (export_run_id, source_file, type_stable_id, table_name)
                VALUES (?, ?, ?, ?)
                """)) {
            for (JpaEntityRecord entity : data.entities) {
                ps.setInt(1, runId);
                ps.setString(2, entity.sourceFile());
                ps.setString(3, entity.typeStableId());
                ps.setString(4, entity.tableName());
                ps.executeUpdate();
            }
        }
    }

    private void insertFields(Connection conn, int runId, JpaSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO jpa_field (export_run_id, type_stable_id, field_name, column_name, id_field)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (JpaFieldRecord field : data.fields) {
                ps.setInt(1, runId);
                ps.setString(2, field.typeStableId());
                ps.setString(3, field.fieldName());
                ps.setString(4, field.columnName());
                ps.setInt(5, field.idField() ? 1 : 0);
                ps.executeUpdate();
            }
        }
    }
}
