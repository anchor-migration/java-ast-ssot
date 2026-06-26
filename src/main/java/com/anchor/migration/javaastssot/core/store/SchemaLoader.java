package com.anchor.migration.javaastssot.core.store;

import com.anchor.migration.javaastssot.profile.ExportProfile;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class SchemaLoader {

    private SchemaLoader() {}

    static void applyCore(Connection conn) throws IOException, SQLException {
        applyResource(conn, "/schema/core/v1.sql");
    }

    static void applyProfiles(Connection conn, Iterable<String> profileIds) throws IOException, SQLException {
        for (String profileId : profileIds) {
            ExportProfile profile = com.anchor.migration.javaastssot.profile.ProfileRegistry.require(profileId);
            applyResource(conn, profile.schemaResource());
        }
    }

    private static void applyResource(Connection conn, String resourcePath) throws IOException, SQLException {
        String ddl;
        try (InputStream in = SchemaLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Schema resource not found: " + resourcePath);
            }
            ddl = new String(in.readAllBytes());
        }
        try (Statement st = conn.createStatement()) {
            for (String stmt : ddl.split(";")) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty()) {
                    st.execute(trimmed);
                }
            }
        }
    }
}
