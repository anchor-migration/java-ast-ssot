package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.core.extract.JavaAstExtractor;
import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreOnlyExportTest {

    @TempDir
    Path tempDir;

    @Test
    void exportCoreOnlyDoesNotCreateProfileTables() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/core-only").toAbsolutePath();
        Path dbPath = tempDir.resolve("core.db");

        var snapshot = new JavaAstExtractor().extract(sourceRoot, Set.of());
        assertTrue(snapshot.enabledProfileIds.isEmpty());
        assertFalse(snapshot.hasProfile("javaee-ejb2-jboss"));
        assertEquals(1, snapshot.javaTypes.size());

        new JavaAstSsotStore().write(dbPath, snapshot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement st = conn.createStatement()) {
            ResultSet profiles =
                    st.executeQuery("SELECT profiles FROM export_run ORDER BY id DESC LIMIT 1");
            assertTrue(profiles.next());
            assertEquals("", profiles.getString(1));

            ResultSet types = st.executeQuery("SELECT COUNT(*) FROM java_type");
            assertTrue(types.next());
            assertEquals(1, types.getInt(1));

            assertFalse(tableExists(conn, "javaee_ejb2_jboss_bean"));
            assertFalse(tableExists(conn, "javaee_ejb2_jboss_crosswalk"));
        }
    }

    @Test
    void unknownProfileFailsFast() {
        Path sourceRoot = Path.of("src/test/resources/core-only").toAbsolutePath();
        assertThrows(
                IllegalArgumentException.class,
                () -> ProfileRegistry.resolve(Collections.singletonList("unknown"), sourceRoot, false));
    }

    private static boolean tableExists(Connection conn, String table) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, table, new String[] {"TABLE"})) {
            return rs.next();
        }
    }
}
