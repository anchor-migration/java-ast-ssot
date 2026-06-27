package com.anchor.migration.javaastssot.crosswalk.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class SchemaSsotReader {

    private final Map<String, String> tableStableIdsByKey = new HashMap<>();
    private final Map<String, String> columnStableIdsByKey = new HashMap<>();

    public static SchemaSsotReader load(Connection conn, int exportRunId) throws SQLException {
        SchemaSsotReader reader = new SchemaSsotReader();
        reader.loadTables(conn, exportRunId);
        reader.loadColumns(conn, exportRunId);
        return reader;
    }

    public Optional<String> resolveTableStableId(String dbSchema, String tableName) {
        return Optional.ofNullable(tableStableIdsByKey.get(tableKey(dbSchema, tableName)));
    }

    public Optional<String> resolveColumnStableId(String dbSchema, String tableName, String columnName) {
        return Optional.ofNullable(columnStableIdsByKey.get(columnKey(dbSchema, tableName, columnName)));
    }

    private void loadTables(Connection conn, int exportRunId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT t.stable_id, s.name AS schema_name, t.name AS table_name
                FROM db_table t
                JOIN db_schema s ON t.schema_id = s.id
                WHERE t.export_run_id = ?
                """)) {
            ps.setInt(1, exportRunId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String schemaName = rs.getString("schema_name");
                    String tableName = rs.getString("table_name");
                    tableStableIdsByKey.put(tableKey(schemaName, tableName), rs.getString("stable_id"));
                }
            }
        }
    }

    private void loadColumns(Connection conn, int exportRunId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT c.stable_id, s.name AS schema_name, t.name AS table_name, c.name AS column_name
                FROM db_column c
                JOIN db_table t ON c.table_id = t.id
                JOIN db_schema s ON t.schema_id = s.id
                WHERE c.export_run_id = ?
                """)) {
            ps.setInt(1, exportRunId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String schemaName = rs.getString("schema_name");
                    String tableName = rs.getString("table_name");
                    String columnName = rs.getString("column_name");
                    columnStableIdsByKey.put(
                            columnKey(schemaName, tableName, columnName), rs.getString("stable_id"));
                }
            }
        }
    }

    static String tableKey(String dbSchema, String tableName) {
        return dbSchema.toLowerCase(Locale.ROOT) + "." + tableName.toLowerCase(Locale.ROOT);
    }

    static String columnKey(String dbSchema, String tableName, String columnName) {
        return tableKey(dbSchema, tableName) + "." + columnName.toLowerCase(Locale.ROOT);
    }
}
