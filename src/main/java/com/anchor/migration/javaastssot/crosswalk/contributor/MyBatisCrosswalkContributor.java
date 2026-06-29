package com.anchor.migration.javaastssot.crosswalk.contributor;

import com.anchor.migration.javaastssot.core.StableIds;
import com.anchor.migration.javaastssot.crosswalk.CrosswalkContext;
import com.anchor.migration.javaastssot.crosswalk.EdgeKinds;
import com.anchor.migration.javaastssot.crosswalk.MappingRoles;
import com.anchor.migration.javaastssot.crosswalk.alignment.AlignmentEvaluator;
import com.anchor.migration.javaastssot.crosswalk.alignment.LinkAlignment;
import com.anchor.migration.javaastssot.crosswalk.model.CodeSchemaLinkRecord;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkContribution;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkIssue;
import com.anchor.migration.javaastssot.crosswalk.schema.SchemaColumnInfo;
import com.anchor.migration.javaastssot.profile.mybatis.MyBatisIds;
import com.anchor.migration.javaastssot.profile.mybatis.MyBatisProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MyBatisCrosswalkContributor implements CrosswalkContributor {

    @Override
    public String profileId() {
        return MyBatisProfile.ID;
    }

    @Override
    public CrosswalkContribution contribute(CrosswalkContext context) throws SQLException {
        List<CodeSchemaLinkRecord> links = new ArrayList<>();
        List<CrosswalkIssue> issues = new ArrayList<>();
        Set<String> javaTypes = loadJavaTypeIds(context.codeConn(), context.codeExportRunId());
        Map<String, String> javaFieldTypes = loadJavaFieldTypes(context.codeConn(), context.codeExportRunId());

        contributePersistentEntities(context, javaTypes, javaFieldTypes, links, issues);
        contributeReadModels(context, javaTypes, javaFieldTypes, links, issues);
        return new CrosswalkContribution(links, issues);
    }

    private void contributePersistentEntities(
            CrosswalkContext context,
            Set<String> javaTypes,
            Map<String, String> javaFieldTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT source_file, result_map_id, type_stable_id, table_name
                FROM mybatis_result_map
                WHERE export_run_id = ?
                  AND mapping_role = ?
                  AND table_name IS NOT NULL
                """)) {
            ps.setInt(1, context.codeExportRunId());
            ps.setString(2, MappingRoles.PERSISTENT_ENTITY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributePersistentEntity(context, rs, javaTypes, links, issues);
                }
            }
        }

        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT m.type_stable_id, f.property_name, f.column_name, m.table_name, m.source_file
                FROM mybatis_result_field f
                JOIN mybatis_result_map m
                  ON m.export_run_id = f.export_run_id
                 AND m.source_file = f.source_file
                 AND m.result_map_id = f.result_map_id
                WHERE f.export_run_id = ?
                  AND m.mapping_role = ?
                  AND m.table_name IS NOT NULL
                """)) {
            ps.setInt(1, context.codeExportRunId());
            ps.setString(2, MappingRoles.PERSISTENT_ENTITY);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributePersistentField(context, rs, javaFieldTypes, links, issues);
                }
            }
        }
    }

    private void contributeReadModels(
            CrosswalkContext context,
            Set<String> javaTypes,
            Map<String, String> javaFieldTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT source_file, result_map_id, type_stable_id, statement_id
                FROM mybatis_result_map
                WHERE export_run_id = ?
                  AND mapping_role = ?
                  AND statement_id IS NOT NULL
                """)) {
            ps.setInt(1, context.codeExportRunId());
            ps.setString(2, MappingRoles.READ_MODEL);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributeReadModelType(context, rs, javaTypes, links, issues);
                }
            }
        }

        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT st.source_file, st.statement_id, st.table_name
                FROM mybatis_statement_table st
                JOIN mybatis_statement s
                  ON s.export_run_id = st.export_run_id
                 AND s.source_file = st.source_file
                 AND s.statement_id = st.statement_id
                WHERE st.export_run_id = ?
                  AND s.join_query = 1
                """)) {
            ps.setInt(1, context.codeExportRunId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributeSqlReferencesTable(context, rs, links, issues);
                }
            }
        }

        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT m.type_stable_id, f.property_name, f.column_name, f.table_name, m.source_file, m.statement_id
                FROM mybatis_result_field f
                JOIN mybatis_result_map m
                  ON m.export_run_id = f.export_run_id
                 AND m.source_file = f.source_file
                 AND m.result_map_id = f.result_map_id
                WHERE f.export_run_id = ?
                  AND m.mapping_role = ?
                  AND f.table_name IS NOT NULL
                """)) {
            ps.setInt(1, context.codeExportRunId());
            ps.setString(2, MappingRoles.READ_MODEL);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributeReadModelField(context, rs, javaFieldTypes, links, issues);
                }
            }
        }
    }

    private void contributePersistentEntity(
            CrosswalkContext context,
            ResultSet rs,
            Set<String> javaTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String typeStableId = rs.getString("type_stable_id");
        String tableName = rs.getString("table_name");
        String sourceFile = rs.getString("source_file");

        if (!javaTypes.contains(typeStableId)) {
            issues.add(
                    new CrosswalkIssue(
                            "warning",
                            "java_type_not_found",
                            "MyBatis result type not present in java_type export: " + typeStableId,
                            typeStableId));
        }

        Optional<String> tableStableId = context.schemaReader().resolveTableStableId(context.dbSchema(), tableName);
        if (tableStableId.isEmpty()) {
            issues.add(
                    new CrosswalkIssue(
                            "error",
                            "table_not_found",
                            "No schema table for "
                                    + context.dbSchema()
                                    + "."
                                    + tableName
                                    + " (MyBatis "
                                    + typeStableId
                                    + ")",
                            typeStableId));
            return;
        }

        LinkAlignment alignment =
                AlignmentEvaluator.evaluateNames(simpleNameFromTypeId(typeStableId), tableName, true);
        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.TYPE_MAPS_TO_TABLE,
                        typeStableId,
                        tableStableId.get(),
                        MappingRoles.PERSISTENT_ENTITY,
                        profileId(),
                        "mybatis_xml",
                        sourceFile,
                        "authoritative",
                        alignment));
    }

    private void contributePersistentField(
            CrosswalkContext context,
            ResultSet rs,
            Map<String, String> javaFieldTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String typeStableId = rs.getString("type_stable_id");
        String tableName = rs.getString("table_name");
        String propertyName = rs.getString("property_name");
        String columnName = rs.getString("column_name");
        String sourceFile = rs.getString("source_file");
        contributeFieldToColumn(
                context,
                typeStableId,
                tableName,
                propertyName,
                columnName,
                sourceFile,
                javaFieldTypes,
                MappingRoles.PERSISTENT_ENTITY,
                EdgeKinds.FIELD_MAPS_TO_COLUMN,
                links,
                issues);
    }

    private void contributeReadModelType(
            CrosswalkContext context,
            ResultSet rs,
            Set<String> javaTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String typeStableId = rs.getString("type_stable_id");
        String sourceFile = rs.getString("source_file");
        String statementId = rs.getString("statement_id");
        String sqlStableId = MyBatisIds.sqlStatement(sourceFile, statementId);

        if (!javaTypes.contains(typeStableId)) {
            issues.add(
                    new CrosswalkIssue(
                            "warning",
                            "java_type_not_found",
                            "MyBatis read model type not present in java_type export: " + typeStableId,
                            typeStableId));
        }

        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.TYPE_BACKED_BY_SQL,
                        typeStableId,
                        sqlStableId,
                        MappingRoles.READ_MODEL,
                        profileId(),
                        "mybatis_xml",
                        sourceFile,
                        "authoritative",
                        AlignmentEvaluator.evaluateNames(typeStableId, statementId, false)));
    }

    private void contributeSqlReferencesTable(
            CrosswalkContext context,
            ResultSet rs,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String sourceFile = rs.getString("source_file");
        String statementId = rs.getString("statement_id");
        String tableName = rs.getString("table_name");
        String sqlStableId = MyBatisIds.sqlStatement(sourceFile, statementId);

        Optional<String> tableStableId = context.schemaReader().resolveTableStableId(context.dbSchema(), tableName);
        if (tableStableId.isEmpty()) {
            issues.add(
                    new CrosswalkIssue(
                            "error",
                            "table_not_found",
                            "No schema table for "
                                    + context.dbSchema()
                                    + "."
                                    + tableName
                                    + " (SQL "
                                    + statementId
                                    + ")",
                            sqlStableId));
            return;
        }

        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.SQL_REFERENCES_TABLE,
                        sqlStableId,
                        tableStableId.get(),
                        MappingRoles.READ_MODEL,
                        profileId(),
                        "mybatis_xml",
                        sourceFile,
                        "inferred",
                        AlignmentEvaluator.evaluateNames(statementId, tableName, true)));
    }

    private void contributeReadModelField(
            CrosswalkContext context,
            ResultSet rs,
            Map<String, String> javaFieldTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String typeStableId = rs.getString("type_stable_id");
        String tableName = rs.getString("table_name");
        String propertyName = rs.getString("property_name");
        String columnName = rs.getString("column_name");
        String sourceFile = rs.getString("source_file");
        contributeFieldToColumn(
                context,
                typeStableId,
                tableName,
                propertyName,
                columnName,
                sourceFile,
                javaFieldTypes,
                MappingRoles.READ_MODEL,
                EdgeKinds.FIELD_MAPS_TO_COLUMN_VIA,
                links,
                issues);
    }

    private void contributeFieldToColumn(
            CrosswalkContext context,
            String typeStableId,
            String tableName,
            String propertyName,
            String columnName,
            String sourceFile,
            Map<String, String> javaFieldTypes,
            String mappingRole,
            String edgeKind,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String fieldStableId = StableIds.javaField(typeStableId, propertyName);
        Optional<SchemaColumnInfo> columnInfo =
                context.schemaReader().resolveColumnInfo(context.dbSchema(), tableName, columnName);
        if (columnInfo.isEmpty()) {
            issues.add(
                    new CrosswalkIssue(
                            "error",
                            "column_not_found",
                            "No schema column for "
                                    + context.dbSchema()
                                    + "."
                                    + tableName
                                    + "."
                                    + columnName
                                    + " (property "
                                    + propertyName
                                    + ")",
                            fieldStableId));
            return;
        }

        String javaFieldType = javaFieldTypes.get(fieldStableId);
        LinkAlignment alignment =
                AlignmentEvaluator.evaluateFieldMapping(
                        propertyName, columnInfo.get().columnName(), javaFieldType, columnInfo.get().dataType());

        links.add(
                new CodeSchemaLinkRecord(
                        edgeKind,
                        fieldStableId,
                        columnInfo.get().stableId(),
                        mappingRole,
                        profileId(),
                        "mybatis_xml",
                        sourceFile,
                        "authoritative",
                        alignment));
    }

    private static String simpleNameFromTypeId(String typeId) {
        int dot = typeId.lastIndexOf('.');
        return dot >= 0 ? typeId.substring(dot + 1) : typeId;
    }

    private Set<String> loadJavaTypeIds(Connection conn, int exportRunId) throws SQLException {
        Set<String> ids = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT stable_id FROM java_type WHERE export_run_id = ?")) {
            ps.setInt(1, exportRunId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
            }
        }
        return ids;
    }

    private Map<String, String> loadJavaFieldTypes(Connection conn, int exportRunId) throws SQLException {
        Map<String, String> types = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT stable_id, field_type FROM java_field WHERE export_run_id = ?")) {
            ps.setInt(1, exportRunId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    types.put(rs.getString(1), rs.getString(2));
                }
            }
        }
        return types;
    }
}
