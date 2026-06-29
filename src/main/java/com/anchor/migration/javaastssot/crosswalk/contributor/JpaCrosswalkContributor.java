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
import com.anchor.migration.javaastssot.profile.jpa.JpaProfile;

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

public final class JpaCrosswalkContributor implements CrosswalkContributor {

    @Override
    public String profileId() {
        return JpaProfile.ID;
    }

    @Override
    public CrosswalkContribution contribute(CrosswalkContext context) throws SQLException {
        List<CodeSchemaLinkRecord> links = new ArrayList<>();
        List<CrosswalkIssue> issues = new ArrayList<>();
        Set<String> javaTypes = loadJavaTypeIds(context.codeConn(), context.codeExportRunId());
        Map<String, String> javaFieldTypes = loadJavaFieldTypes(context.codeConn(), context.codeExportRunId());

        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT source_file, type_stable_id, table_name
                FROM jpa_entity
                WHERE export_run_id = ?
                """)) {
            ps.setInt(1, context.codeExportRunId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributeEntity(context, rs, javaTypes, links, issues);
                }
            }
        }

        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT f.type_stable_id, f.field_name, f.column_name, e.table_name, e.source_file
                FROM jpa_field f
                JOIN jpa_entity e
                  ON e.export_run_id = f.export_run_id AND e.type_stable_id = f.type_stable_id
                WHERE f.export_run_id = ?
                """)) {
            ps.setInt(1, context.codeExportRunId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributeField(context, rs, javaFieldTypes, links, issues);
                }
            }
        }

        return new CrosswalkContribution(links, issues);
    }

    private void contributeEntity(
            CrosswalkContext context,
            ResultSet rs,
            Set<String> javaTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String typeStableId = rs.getString("type_stable_id");
        String tableName = rs.getString("table_name");
        String sourceFile = rs.getString("source_file");
        String dbSchema = context.dbSchema();
        String simpleClassName = simpleNameFromTypeId(typeStableId);

        if (!javaTypes.contains(typeStableId)) {
            issues.add(
                    new CrosswalkIssue(
                            "warning",
                            "java_type_not_found",
                            "JPA entity type not present in java_type export: " + typeStableId,
                            typeStableId));
        }

        Optional<String> tableStableId = context.schemaReader().resolveTableStableId(dbSchema, tableName);
        if (tableStableId.isEmpty()) {
            issues.add(
                    new CrosswalkIssue(
                            "error",
                            "table_not_found",
                            "No schema table for " + dbSchema + "." + tableName + " (JPA " + typeStableId + ")",
                            typeStableId));
            return;
        }

        LinkAlignment tableAlignment = AlignmentEvaluator.evaluateNames(simpleClassName, tableName, true);
        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.TYPE_MAPS_TO_TABLE,
                        typeStableId,
                        tableStableId.get(),
                        MappingRoles.PERSISTENT_ENTITY,
                        profileId(),
                        "jpa_annotation",
                        sourceFile,
                        "authoritative",
                        tableAlignment));
    }

    private void contributeField(
            CrosswalkContext context,
            ResultSet rs,
            Map<String, String> javaFieldTypes,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String typeStableId = rs.getString("type_stable_id");
        String tableName = rs.getString("table_name");
        String fieldName = rs.getString("field_name");
        String columnName = rs.getString("column_name");
        String sourceFile = rs.getString("source_file");

        String fieldStableId = StableIds.javaField(typeStableId, fieldName);
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
                                    + " (field "
                                    + fieldName
                                    + ")",
                            fieldStableId));
            return;
        }

        String javaFieldType = javaFieldTypes.get(fieldStableId);
        LinkAlignment alignment =
                AlignmentEvaluator.evaluateFieldMapping(
                        fieldName, columnInfo.get().columnName(), javaFieldType, columnInfo.get().dataType());

        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.FIELD_MAPS_TO_COLUMN,
                        fieldStableId,
                        columnInfo.get().stableId(),
                        MappingRoles.PERSISTENT_ENTITY,
                        profileId(),
                        "jpa_annotation",
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
