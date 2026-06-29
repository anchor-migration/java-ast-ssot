package com.anchor.migration.javaastssot.profile.mybatis;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.profile.ExportProfile;
import com.anchor.migration.javaastssot.profile.ProfileSnapshot;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisResultFieldRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisResultMapRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisStatementRecord;
import com.anchor.migration.javaastssot.profile.mybatis.model.MyBatisStatementTableRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class MyBatisProfile implements ExportProfile {

    public static final String ID = MyBatisIds.PROFILE_ID;

    private final MyBatisMapperXmlExtractor extractor = new MyBatisMapperXmlExtractor();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String schemaResource() {
        return "/schema/profiles/mybatis/v1.sql";
    }

    @Override
    public boolean handlesFileName(String fileName) {
        return fileName.endsWith("Mapper.xml") || fileName.endsWith(".xml");
    }

    @Override
    public boolean detect(Path sourceRoot) {
        try (var walk = Files.walk(sourceRoot)) {
            return walk.filter(Files::isRegularFile).anyMatch(extractor::looksLikeMapper);
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public ProfileSnapshot newSnapshot() {
        return new MyBatisSnapshot();
    }

    @Override
    public void processFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        if (!extractor.looksLikeMapper(file)) {
            return;
        }
        extractor.parseFile(file, sourceRoot, snapshot);
    }

    @Override
    public void writeSql(Connection conn, int exportRunId, ExportSnapshot snapshot) throws SQLException {
        MyBatisSnapshot data = snapshot.requireProfile(ID, MyBatisSnapshot.class);
        insertResultMaps(conn, exportRunId, data);
        insertResultFields(conn, exportRunId, data);
        insertStatements(conn, exportRunId, data);
        insertStatementTables(conn, exportRunId, data);
    }

    @Override
    public ProfileStats stats(ExportSnapshot snapshot) {
        if (!snapshot.hasProfile(ID)) {
            return new ProfileStats(0, 0);
        }
        MyBatisSnapshot data = snapshot.requireProfile(ID, MyBatisSnapshot.class);
        long crosswalkFacts = data.resultMaps.size() + data.resultFields.size() + data.statements.size();
        return new ProfileStats(data.resultMaps.size(), (int) crosswalkFacts);
    }

    @Override
    public ProfileStats readStats(Connection conn, int exportRunId) throws SQLException {
        return new ProfileStats(
                count(conn, "mybatis_result_map", exportRunId),
                count(conn, "mybatis_result_field", exportRunId)
                        + count(conn, "mybatis_statement", exportRunId));
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

    private void insertResultMaps(Connection conn, int runId, MyBatisSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO mybatis_result_map
                    (export_run_id, source_file, result_map_id, type_stable_id, mapping_role, table_name, statement_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (MyBatisResultMapRecord map : data.resultMaps) {
                ps.setInt(1, runId);
                ps.setString(2, map.sourceFile());
                ps.setString(3, map.resultMapId());
                ps.setString(4, map.typeStableId());
                ps.setString(5, map.mappingRole());
                ps.setString(6, map.tableName());
                ps.setString(7, map.statementId());
                ps.executeUpdate();
            }
        }
    }

    private void insertResultFields(Connection conn, int runId, MyBatisSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO mybatis_result_field
                    (export_run_id, source_file, result_map_id, property_name, column_name, table_name)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (MyBatisResultFieldRecord field : data.resultFields) {
                ps.setInt(1, runId);
                ps.setString(2, field.sourceFile());
                ps.setString(3, field.resultMapId());
                ps.setString(4, field.propertyName());
                ps.setString(5, field.columnName());
                ps.setString(6, field.tableName());
                ps.executeUpdate();
            }
        }
    }

    private void insertStatements(Connection conn, int runId, MyBatisSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO mybatis_statement
                    (export_run_id, source_file, statement_id, statement_type, result_map_id,
                     result_type_stable_id, sql_text, join_query, mapping_role)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (MyBatisStatementRecord statement : data.statements) {
                ps.setInt(1, runId);
                ps.setString(2, statement.sourceFile());
                ps.setString(3, statement.statementId());
                ps.setString(4, statement.statementType());
                ps.setString(5, statement.resultMapId());
                ps.setString(6, statement.resultTypeStableId());
                ps.setString(7, statement.sqlText());
                ps.setInt(8, statement.joinQuery() ? 1 : 0);
                ps.setString(9, statement.mappingRole());
                ps.executeUpdate();
            }
        }
    }

    private void insertStatementTables(Connection conn, int runId, MyBatisSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO mybatis_statement_table
                    (export_run_id, source_file, statement_id, table_name)
                VALUES (?, ?, ?, ?)
                """)) {
            for (MyBatisStatementTableRecord table : data.statementTables) {
                ps.setInt(1, runId);
                ps.setString(2, table.sourceFile());
                ps.setString(3, table.statementId());
                ps.setString(4, table.tableName());
                ps.executeUpdate();
            }
        }
    }
}
