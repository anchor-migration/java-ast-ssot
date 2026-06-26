package com.anchor.migration.codessot.store;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

final class SchemaRunner {

    private SchemaRunner() {}

    static void applyCoreSchema(Connection conn) throws IOException, SQLException {
        applyResource(conn, "/schema/v1-core.sql");
    }

    static void applyProfileSchemas(Connection conn, Iterable<String> profileIds) throws IOException, SQLException {
        for (String profileId : profileIds) {
            String resource = profileSchemaResource(profileId);
            applyResource(conn, resource);
        }
    }

    private static String profileSchemaResource(String profileId) {
        return switch (profileId) {
            case "javaee-ejb2-jboss" -> "/schema/profile-javaee-ejb2-jboss.sql";
            default -> throw new IllegalArgumentException("No schema resource for profile: " + profileId);
        };
    }

    private static void applyResource(Connection conn, String resourcePath) throws IOException, SQLException {
        String ddl;
        try (InputStream in = SchemaRunner.class.getResourceAsStream(resourcePath)) {
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
