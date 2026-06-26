package com.anchor.migration.javaastssot.profile;

import com.anchor.migration.javaastssot.core.model.ExportSnapshot;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/** Stack-specific export extension (Java EE, Spring, …). */
public interface ExportProfile {

    String id();

    String schemaResource();

    boolean handlesFileName(String fileName);

    boolean detect(Path sourceRoot);

    ProfileSnapshot newSnapshot();

    void processFile(Path file, Path sourceRoot, ExportSnapshot snapshot) throws Exception;

    void writeSql(Connection conn, int exportRunId, ExportSnapshot snapshot) throws SQLException;

    ProfileStats stats(ExportSnapshot snapshot);

    ProfileStats readStats(Connection conn, int exportRunId) throws SQLException;

    record ProfileStats(int entityCount, int crosswalkCount) {}
}
