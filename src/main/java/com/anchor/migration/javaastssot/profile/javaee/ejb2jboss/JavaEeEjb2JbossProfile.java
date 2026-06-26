package com.anchor.migration.javaastssot.profile.javaee.ejb2jboss;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;
import com.anchor.migration.javaastssot.profile.ExportProfile;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import com.anchor.migration.javaastssot.profile.ProfileSnapshot;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.CrosswalkEdgeRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbBeanRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbCmpFieldRecord;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.model.EjbRefRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class JavaEeEjb2JbossProfile implements ExportProfile {

    public static final String ID = JavaEeEjb2JbossIds.PROFILE_ID;

    private final EjbDescriptorExtractor extractor = new EjbDescriptorExtractor();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String schemaResource() {
        return "/schema/profiles/javaee-ejb2-jboss/v1.sql";
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
    public ProfileSnapshot newSnapshot() {
        return new JavaEeEjb2JbossSnapshot();
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
        JavaEeEjb2JbossSnapshot data = snapshot.requireProfile(ID, JavaEeEjb2JbossSnapshot.class);
        insertBeans(conn, exportRunId, data);
        insertCmpFields(conn, exportRunId, data);
        insertRefs(conn, exportRunId, data);
        insertCrosswalk(conn, exportRunId, data);
    }

    @Override
    public ProfileStats stats(ExportSnapshot snapshot) {
        if (!snapshot.hasProfile(ID)) {
            return new ProfileStats(0, 0);
        }
        JavaEeEjb2JbossSnapshot data = snapshot.requireProfile(ID, JavaEeEjb2JbossSnapshot.class);
        return new ProfileStats(data.beans.size(), data.crosswalk.size());
    }

    @Override
    public ProfileStats readStats(Connection conn, int exportRunId) throws SQLException {
        return new ProfileStats(count(conn, "javaee_ejb2_jboss_bean", exportRunId), count(conn, "javaee_ejb2_jboss_crosswalk", exportRunId));
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

    private void insertBeans(Connection conn, int runId, JavaEeEjb2JbossSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO javaee_ejb2_jboss_bean (export_run_id, descriptor_file, ejb_name, ejb_class,
                    bean_type, session_type, persistence_type, table_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (EjbBeanRecord bean : data.beans) {
                ps.setInt(1, runId);
                ps.setString(2, bean.descriptorFile());
                ps.setString(3, bean.ejbName());
                ps.setString(4, bean.ejbClass());
                ps.setString(5, bean.beanType());
                ps.setString(6, bean.sessionType());
                ps.setString(7, bean.persistenceType());
                ps.setString(8, bean.tableName());
                ps.executeUpdate();
            }
        }
    }

    private void insertCmpFields(Connection conn, int runId, JavaEeEjb2JbossSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO javaee_ejb2_jboss_cmp_field (export_run_id, ejb_name, field_name, column_name)
                VALUES (?, ?, ?, ?)
                """)) {
            for (EjbCmpFieldRecord field : data.cmpFields) {
                ps.setInt(1, runId);
                ps.setString(2, field.ejbName());
                ps.setString(3, field.fieldName());
                ps.setString(4, field.columnName());
                ps.executeUpdate();
            }
        }
    }

    private void insertRefs(Connection conn, int runId, JavaEeEjb2JbossSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO javaee_ejb2_jboss_ref (export_run_id, source_ejb_name, ref_name, ref_type, ejb_link, ref_kind)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (EjbRefRecord ref : data.refs) {
                ps.setInt(1, runId);
                ps.setString(2, ref.sourceEjbName());
                ps.setString(3, ref.refName());
                ps.setString(4, ref.refType());
                ps.setString(5, ref.ejbLink());
                ps.setString(6, ref.refKind());
                ps.executeUpdate();
            }
        }
    }

    private void insertCrosswalk(Connection conn, int runId, JavaEeEjb2JbossSnapshot data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT OR IGNORE INTO javaee_ejb2_jboss_crosswalk
                    (export_run_id, edge_kind, source_stable_id, target_stable_id)
                VALUES (?, ?, ?, ?)
                """)) {
            for (CrosswalkEdgeRecord edge : data.crosswalk) {
                ps.setInt(1, runId);
                ps.setString(2, edge.edgeKind());
                ps.setString(3, edge.sourceStableId());
                ps.setString(4, edge.targetStableId());
                ps.executeUpdate();
            }
        }
    }
}
