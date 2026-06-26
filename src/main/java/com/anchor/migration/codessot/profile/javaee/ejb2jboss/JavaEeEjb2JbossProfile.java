package com.anchor.migration.codessot.profile.javaee.ejb2jboss;

import com.anchor.migration.codessot.model.CrosswalkEdgeRecord;
import com.anchor.migration.codessot.model.EjbBeanRecord;
import com.anchor.migration.codessot.model.EjbCmpFieldRecord;
import com.anchor.migration.codessot.model.EjbRefRecord;
import com.anchor.migration.codessot.model.ExportSnapshot;
import com.anchor.migration.codessot.model.profile.JavaEeEjb2JbossSnapshot;
import com.anchor.migration.codessot.profile.ExportProfile;
import com.anchor.migration.codessot.profile.ProfileRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class JavaEeEjb2JbossProfile implements ExportProfile {

    public static final String ID = "javaee-ejb2-jboss";
    public static final String DEFAULT_DB_SCHEMA = "dukesbank";

    private final EjbDescriptorExtractor extractor = new EjbDescriptorExtractor();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String schemaResource() {
        return "/schema/profile-javaee-ejb2-jboss.sql";
    }

    @Override
    public boolean handlesFileName(String fileName) {
        return "ejb-jar.xml".equals(fileName) || "jbosscmp-jdbc.xml".equals(fileName);
    }

    @Override
    public boolean detect(Path sourceRoot) {
        try {
            return ProfileRegistry.containsHandledFile(sourceRoot, this);
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public void processFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception {
        String name = file.getFileName().toString();
        if ("ejb-jar.xml".equals(name)) {
            extractor.parseEjbJar(file, sourceRoot, snapshot);
        } else if ("jbosscmp-jdbc.xml".equals(name)) {
            extractor.parseJbossCmpJdbc(file, sourceRoot, snapshot);
        }
    }

    @Override
    public void writeSql(Connection conn, int exportRunId, ExportSnapshot snapshot) throws SQLException {
        JavaEeEjb2JbossSnapshot data = snapshot.javaEeEjb2Jboss;
        if (data == null) {
            return;
        }
        insertEjbBeans(conn, exportRunId, data);
        insertEjbCmpFields(conn, exportRunId, data);
        insertEjbRefs(conn, exportRunId, data);
        insertCrosswalk(conn, exportRunId, data);
    }

    @Override
    public boolean isActive(ExportSnapshot snapshot) {
        return snapshot.javaEeEjb2Jboss != null;
    }

    @Override
    public ProfileCounts counts(ExportSnapshot snapshot) {
        if (snapshot.javaEeEjb2Jboss == null) {
            return new ProfileCounts(0, 0);
        }
        return new ProfileCounts(
                snapshot.javaEeEjb2Jboss.ejbBeans.size(),
                snapshot.javaEeEjb2Jboss.crosswalkEdges.size());
    }

    private void insertEjbBeans(Connection conn, int runId, JavaEeEjb2JbossSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO ejb_bean (export_run_id, profile_id, descriptor_file, ejb_name, ejb_class,
                    bean_type, session_type, persistence_type, table_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (EjbBeanRecord bean : data.ejbBeans) {
                ps.setInt(1, runId);
                ps.setString(2, ID);
                ps.setString(3, bean.descriptorFile());
                ps.setString(4, bean.ejbName());
                ps.setString(5, bean.ejbClass());
                ps.setString(6, bean.beanType());
                ps.setString(7, bean.sessionType());
                ps.setString(8, bean.persistenceType());
                ps.setString(9, bean.tableName());
                ps.executeUpdate();
            }
        }
    }

    private void insertEjbCmpFields(Connection conn, int runId, JavaEeEjb2JbossSnapshot data)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO ejb_cmp_field (export_run_id, profile_id, ejb_name, field_name, column_name)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (EjbCmpFieldRecord field : data.ejbCmpFields) {
                ps.setInt(1, runId);
                ps.setString(2, ID);
                ps.setString(3, field.ejbName());
                ps.setString(4, field.fieldName());
                ps.setString(5, field.columnName());
                ps.executeUpdate();
            }
        }
    }

    private void insertEjbRefs(Connection conn, int runId, JavaEeEjb2JbossSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO ejb_ref (export_run_id, profile_id, source_ejb_name, ref_name, ref_type, ejb_link, ref_kind)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (EjbRefRecord ref : data.ejbRefs) {
                ps.setInt(1, runId);
                ps.setString(2, ID);
                ps.setString(3, ref.sourceEjbName());
                ps.setString(4, ref.refName());
                ps.setString(5, ref.refType());
                ps.setString(6, ref.ejbLink());
                ps.setString(7, ref.refKind());
                ps.executeUpdate();
            }
        }
    }

    private void insertCrosswalk(Connection conn, int runId, JavaEeEjb2JbossSnapshot data)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT OR IGNORE INTO profile_crosswalk_edge
                    (export_run_id, profile_id, edge_kind, source_stable_id, target_stable_id)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            for (CrosswalkEdgeRecord edge : data.crosswalkEdges) {
                ps.setInt(1, runId);
                ps.setString(2, ID);
                ps.setString(3, edge.edgeKind());
                ps.setString(4, edge.sourceStableId());
                ps.setString(5, edge.targetStableId());
                ps.executeUpdate();
            }
        }
    }
}
