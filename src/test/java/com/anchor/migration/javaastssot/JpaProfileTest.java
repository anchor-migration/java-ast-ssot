package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.core.extract.JavaAstExtractor;
import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import com.anchor.migration.javaastssot.profile.jpa.JpaProfile;
import com.anchor.migration.javaastssot.profile.jpa.JpaSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JpaProfileTest {

    @TempDir
    Path tempDir;

    @Test
    void exportWithProfileWritesNamespacedTables() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/jpa").toAbsolutePath();
        Path dbPath = tempDir.resolve("jpa.db");

        var snapshot = new JavaAstExtractor().extract(sourceRoot, Set.of(JpaProfile.ID));
        JpaSnapshot data = snapshot.requireProfile(JpaProfile.ID, JpaSnapshot.class);
        assertEquals(1, snapshot.javaTypes.size());
        assertEquals(1, data.entities.size());
        assertEquals(1, data.fields.size());
        assertEquals("com.example.AccountBean", data.entities.get(0).typeStableId());
        assertEquals("ACCOUNT", data.entities.get(0).tableName());
        assertEquals("accountId", data.fields.get(0).fieldName());
        assertEquals("ACCOUNT_ID", data.fields.get(0).columnName());
        assertTrue(data.fields.get(0).idField());

        new JavaAstSsotStore().write(dbPath, snapshot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement st = conn.createStatement()) {
            ResultSet profiles =
                    st.executeQuery("SELECT profiles FROM export_run ORDER BY id DESC LIMIT 1");
            assertTrue(profiles.next());
            assertEquals(JpaProfile.ID, profiles.getString(1));

            ResultSet entities = st.executeQuery("SELECT COUNT(*) FROM jpa_entity");
            assertTrue(entities.next());
            assertEquals(1, entities.getInt(1));

            ResultSet fields = st.executeQuery("SELECT COUNT(*) FROM jpa_field");
            assertTrue(fields.next());
            assertEquals(1, fields.getInt(1));
        }
    }

    @Test
    void autoDetectEnablesProfileWhenEntityAnnotationsPresent() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/jpa").toAbsolutePath();
        var detected = ProfileRegistry.resolve(List.of(), sourceRoot, true);
        assertEquals(Set.of(JpaProfile.ID), detected);
    }
}
