package com.anchor.migration.codessot.profile;

import com.anchor.migration.codessot.model.ExportSnapshot;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/** Stack-specific export extension (Java EE, Spring, …). */
public interface ExportProfile {

    String id();

    /** Classpath resource for profile DDL, e.g. {@code /schema/profile-javaee-ejb2-jboss.sql}. */
    String schemaResource();

    boolean handlesFileName(String fileName);

    /** Returns true when descriptor files for this profile exist under {@code sourceRoot}. */
    boolean detect(Path sourceRoot);

    void processFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception;

    void writeSql(Connection conn, int exportRunId, ExportSnapshot snapshot) throws SQLException;

    boolean isActive(ExportSnapshot snapshot);

    ProfileCounts counts(ExportSnapshot snapshot);

    record ProfileCounts(int primaryEntityCount, int crosswalkEdgeCount) {}
}
