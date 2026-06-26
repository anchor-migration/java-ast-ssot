package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.core.extract.JavaAstExtractor;
import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.JavaEeEjb2JbossProfile;
import com.anchor.migration.javaastssot.profile.javaee.ejb2jboss.JavaEeEjb2JbossSnapshot;
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

class JavaEeEjb2JbossProfileTest {

    @TempDir
    Path tempDir;

    @Test
    void exportWithProfileWritesNamespacedTables() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/javaee-ejb2-jboss").toAbsolutePath();
        Path dbPath = tempDir.resolve("javaee.db");

        var snapshot = new JavaAstExtractor().extract(sourceRoot, Set.of(JavaEeEjb2JbossProfile.ID));
        JavaEeEjb2JbossSnapshot data = snapshot.requireProfile(JavaEeEjb2JbossProfile.ID, JavaEeEjb2JbossSnapshot.class);
        assertEquals(1, snapshot.javaTypes.size());
        assertEquals(1, data.beans.size());
        assertEquals(2, data.crosswalk.size());

        new JavaAstSsotStore().write(dbPath, snapshot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement st = conn.createStatement()) {
            ResultSet profiles =
                    st.executeQuery("SELECT profiles FROM export_run ORDER BY id DESC LIMIT 1");
            assertTrue(profiles.next());
            assertEquals(JavaEeEjb2JbossProfile.ID, profiles.getString(1));

            ResultSet beans = st.executeQuery("SELECT COUNT(*) FROM javaee_ejb2_jboss_bean");
            assertTrue(beans.next());
            assertEquals(1, beans.getInt(1));

            ResultSet crosswalk = st.executeQuery("SELECT COUNT(*) FROM javaee_ejb2_jboss_crosswalk");
            assertTrue(crosswalk.next());
            assertEquals(2, crosswalk.getInt(1));
        }
    }

    @Test
    void autoDetectEnablesProfileWhenDescriptorsPresent() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/javaee-ejb2-jboss").toAbsolutePath();
        var detected = ProfileRegistry.resolve(List.of(), sourceRoot, true);
        assertEquals(Set.of(JavaEeEjb2JbossProfile.ID), detected);
    }
}
