package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.core.extract.JavaAstExtractor;
import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import com.anchor.migration.javaastssot.crosswalk.CrosswalkLinkEngine;
import com.anchor.migration.javaastssot.crosswalk.CrosswalkLinkStore;
import com.anchor.migration.javaastssot.crosswalk.EdgeKinds;
import com.anchor.migration.javaastssot.crosswalk.MappingRoles;
import com.anchor.migration.javaastssot.crosswalk.model.CrosswalkLinkResult;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.JavaEeEjb2JbossProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CrosswalkLinkTest {

    private static final String DB_SCHEMA = "dukesbank";

    @TempDir
    Path tempDir;

    @Test
    void linksJavaEeEntityToSchemaTablesAndColumns() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/javaee-ejb2-jboss").toAbsolutePath();
        Path codeDb = tempDir.resolve("code.db");
        Path schemaDb = tempDir.resolve("schema.db");
        Path linkedDb = tempDir.resolve("linked.db");

        var snapshot = new JavaAstExtractor().extract(sourceRoot, Set.of(JavaEeEjb2JbossProfile.ID));
        new JavaAstSsotStore().write(codeDb, snapshot);
        createMinimalSchemaDb(schemaDb);

        CrosswalkLinkResult result =
                new CrosswalkLinkEngine().link(codeDb, schemaDb, linkedDb, DB_SCHEMA, null, null);

        assertEquals(0, result.errorCount());
        assertEquals(3, result.linkCount());
        assertEquals(JavaEeEjb2JbossProfile.ID, result.profilesLinked().get(0));

        CrosswalkLinkStore.CrosswalkSummary summary = new CrosswalkLinkStore().readSummary(linkedDb);
        assertEquals(3, summary.linkCount());
        assertEquals(0, summary.errorCount());

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + linkedDb);
                Statement st = conn.createStatement()) {
            assertLinkExists(
                    st,
                    EdgeKinds.STACK_BRIDGE,
                    "com.example.AccountBean",
                    "ejb:AccountBean",
                    MappingRoles.PERSISTENT_ENTITY);
            assertLinkExists(
                    st,
                    EdgeKinds.TYPE_MAPS_TO_TABLE,
                    "com.example.AccountBean",
                    DB_SCHEMA + ".ACCOUNT",
                    MappingRoles.PERSISTENT_ENTITY);
            assertLinkExists(
                    st,
                    EdgeKinds.FIELD_MAPS_TO_COLUMN,
                    "com.example.AccountBean#accountId",
                    DB_SCHEMA + ".ACCOUNT.ACCOUNT_ID",
                    MappingRoles.PERSISTENT_ENTITY);
        }
    }

    @Test
    void reportsErrorWhenSchemaTableMissing() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/javaee-ejb2-jboss").toAbsolutePath();
        Path codeDb = tempDir.resolve("code.db");
        Path schemaDb = tempDir.resolve("empty-schema.db");
        Path linkedDb = tempDir.resolve("linked.db");

        var snapshot = new JavaAstExtractor().extract(sourceRoot, Set.of(JavaEeEjb2JbossProfile.ID));
        new JavaAstSsotStore().write(codeDb, snapshot);
        createEmptySchemaDb(schemaDb);

        CrosswalkLinkResult result =
                new CrosswalkLinkEngine().link(codeDb, schemaDb, linkedDb, DB_SCHEMA, null, null);

        assertTrue(result.errorCount() >= 1);
        assertTrue(result.linkCount() >= 1);

        CrosswalkLinkStore.CrosswalkSummary summary = new CrosswalkLinkStore().readSummary(linkedDb);
        assertTrue(summary.errorCount() >= 1);
    }

    private static void assertLinkExists(
            Statement st, String edgeKind, String sourceId, String targetId, String mappingRole)
            throws Exception {
        ResultSet rs =
                st.executeQuery(
                        """
                        SELECT COUNT(*) FROM code_schema_link
                        WHERE edge_kind = '%s'
                          AND source_stable_id = '%s'
                          AND target_stable_id = '%s'
                          AND mapping_role = '%s'
                        """
                                .formatted(edgeKind, sourceId, targetId, mappingRole));
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
    }

    private static void createMinimalSchemaDb(Path schemaDb) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + schemaDb);
                Statement st = conn.createStatement()) {
            st.execute(
                    """
                    CREATE TABLE export_run (
                        id INTEGER PRIMARY KEY, source_dialect TEXT, source_url_masked TEXT,
                        exported_at TEXT, tool_version TEXT
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE db_schema (
                        id INTEGER PRIMARY KEY, export_run_id INTEGER, name TEXT,
                        UNIQUE (export_run_id, name)
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE db_table (
                        id INTEGER PRIMARY KEY, schema_id INTEGER, export_run_id INTEGER,
                        name TEXT, table_type TEXT, stable_id TEXT,
                        UNIQUE (export_run_id, stable_id)
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE db_column (
                        id INTEGER PRIMARY KEY, table_id INTEGER, export_run_id INTEGER,
                        name TEXT, ordinal INTEGER, data_type TEXT, full_type TEXT,
                        nullable INTEGER, stable_id TEXT,
                        UNIQUE (export_run_id, stable_id)
                    )
                    """);
            st.execute(
                    "INSERT INTO export_run VALUES (1, 'mysql', 'jdbc:mysql://test', datetime('now'), 'test')");
            st.execute("INSERT INTO db_schema VALUES (1, 1, 'dukesbank')");
            st.execute(
                    "INSERT INTO db_table VALUES (1, 1, 1, 'ACCOUNT', 'TABLE', 'dukesbank.ACCOUNT')");
            st.execute(
                    """
                    INSERT INTO db_column VALUES (1, 1, 1, 'ACCOUNT_ID', 1, 'varchar', 'varchar(10)', 0,
                        'dukesbank.ACCOUNT.ACCOUNT_ID')
                    """);
        }
    }

    private static void createEmptySchemaDb(Path schemaDb) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + schemaDb);
                Statement st = conn.createStatement()) {
            st.execute(
                    """
                    CREATE TABLE export_run (
                        id INTEGER PRIMARY KEY, source_dialect TEXT, source_url_masked TEXT,
                        exported_at TEXT, tool_version TEXT
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE db_schema (
                        id INTEGER PRIMARY KEY, export_run_id INTEGER, name TEXT,
                        UNIQUE (export_run_id, name)
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE db_table (
                        id INTEGER PRIMARY KEY, schema_id INTEGER, export_run_id INTEGER,
                        name TEXT, table_type TEXT, stable_id TEXT,
                        UNIQUE (export_run_id, stable_id)
                    )
                    """);
            st.execute(
                    """
                    CREATE TABLE db_column (
                        id INTEGER PRIMARY KEY, table_id INTEGER, export_run_id INTEGER,
                        name TEXT, ordinal INTEGER, data_type TEXT, full_type TEXT,
                        nullable INTEGER, stable_id TEXT,
                        UNIQUE (export_run_id, stable_id)
                    )
                    """);
            st.execute(
                    "INSERT INTO export_run VALUES (1, 'mysql', 'jdbc:mysql://test', datetime('now'), 'test')");
            st.execute("INSERT INTO db_schema VALUES (1, 1, 'dukesbank')");
        }
    }
}
