package com.anchor.migration.javaastssot.crosswalk.contributor;

import com.anchor.migration.javaastssot.core.StableIds;
import com.anchor.migration.javaastssot.crosswalk.EdgeKinds;
import com.anchor.migration.javaastssot.crosswalk.MappingRoles;
import com.anchor.migration.javaastssot.crosswalk.CrosswalkContext;
import com.anchor.migration.javaastssot.crosswalk.model.CodeSchemaLinkRecord;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkContribution;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkIssue;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.JavaEeEjb2JbossIds;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.JavaEeEjb2JbossProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class JavaEeEjb2JbossCrosswalkContributor implements CrosswalkContributor {

    @Override
    public String profileId() {
        return JavaEeEjb2JbossProfile.ID;
    }

    @Override
    public CrosswalkContribution contribute(CrosswalkContext context) throws SQLException {
        List<CodeSchemaLinkRecord> links = new ArrayList<>();
        List<CrosswalkIssue> issues = new ArrayList<>();
        Set<String> javaTypes = loadJavaTypeIds(context.codeConn(), context.codeExportRunId());

        try (PreparedStatement ps = context.codeConn().prepareStatement(
                """
                SELECT ejb_name, ejb_class, table_name, bean_type, descriptor_file
                FROM javaee_ejb2_jboss_bean
                WHERE export_run_id = ?
                  AND bean_type = 'entity'
                  AND table_name IS NOT NULL
                  AND ejb_class IS NOT NULL
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
                SELECT f.ejb_name, f.field_name, f.column_name, b.ejb_class, b.table_name
                FROM javaee_ejb2_jboss_cmp_field f
                JOIN javaee_ejb2_jboss_bean b
                  ON b.export_run_id = f.export_run_id AND b.ejb_name = f.ejb_name
                WHERE f.export_run_id = ?
                  AND b.bean_type = 'entity'
                  AND b.table_name IS NOT NULL
                  AND b.ejb_class IS NOT NULL
                """)) {
            ps.setInt(1, context.codeExportRunId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributeCmpField(context, rs, links, issues);
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
        String ejbName = rs.getString("ejb_name");
        String ejbClass = rs.getString("ejb_class");
        String tableName = rs.getString("table_name");
        String descriptorFile = rs.getString("descriptor_file");
        String dbSchema = context.dbSchema();

        if (!javaTypes.contains(ejbClass)) {
            issues.add(
                    new CrosswalkIssue(
                            "warning",
                            "java_type_not_found",
                            "EJB class not present in java_type export: " + ejbClass,
                            ejbClass));
        }

        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.STACK_BRIDGE,
                        ejbClass,
                        JavaEeEjb2JbossIds.ejbBean(ejbName),
                        MappingRoles.PERSISTENT_ENTITY,
                        profileId(),
                        "ejb_jar_xml",
                        descriptorFile,
                        "authoritative"));

        Optional<String> tableStableId = context.schemaReader().resolveTableStableId(dbSchema, tableName);
        if (tableStableId.isEmpty()) {
            issues.add(
                    new CrosswalkIssue(
                            "error",
                            "table_not_found",
                            "No schema table for " + dbSchema + "." + tableName + " (EJB " + ejbName + ")",
                            JavaEeEjb2JbossIds.dbTable(dbSchema, tableName)));
            return;
        }

        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.TYPE_MAPS_TO_TABLE,
                        ejbClass,
                        tableStableId.get(),
                        MappingRoles.PERSISTENT_ENTITY,
                        profileId(),
                        "jbosscmp_xml",
                        "jbosscmp-jdbc.xml",
                        "authoritative"));
    }

    private void contributeCmpField(
            CrosswalkContext context,
            ResultSet rs,
            List<CodeSchemaLinkRecord> links,
            List<CrosswalkIssue> issues)
            throws SQLException {
        String ejbClass = rs.getString("ejb_class");
        String tableName = rs.getString("table_name");
        String fieldName = rs.getString("field_name");
        String columnName = rs.getString("column_name");
        if (columnName == null || columnName.isBlank()) {
            columnName = fieldName;
        }

        String fieldStableId = StableIds.javaField(ejbClass, fieldName);
        Optional<String> columnStableId =
                context.schemaReader().resolveColumnStableId(context.dbSchema(), tableName, columnName);
        if (columnStableId.isEmpty()) {
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

        links.add(
                new CodeSchemaLinkRecord(
                        EdgeKinds.FIELD_MAPS_TO_COLUMN,
                        fieldStableId,
                        columnStableId.get(),
                        MappingRoles.PERSISTENT_ENTITY,
                        profileId(),
                        "jbosscmp_xml",
                        "jbosscmp-jdbc.xml",
                        "authoritative"));
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
}
