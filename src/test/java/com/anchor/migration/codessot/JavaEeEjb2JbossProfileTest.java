package com.anchor.migration.codessot;

import com.anchor.migration.codessot.extract.CodeExtractor;
import com.anchor.migration.codessot.profile.ProfileRegistry;
import com.anchor.migration.codessot.profile.javaee.ejb2jboss.JavaEeEjb2JbossProfile;
import com.anchor.migration.codessot.store.CodeSsotWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JavaEeEjb2JbossProfileTest {

    @TempDir
    Path tempDir;

    @Test
    void exportWithProfileWritesEjbAndCrosswalkTables() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/javaee-ejb2-jboss").toAbsolutePath();
        Path dbPath = tempDir.resolve("javaee.db");

        var snapshot =
                new CodeExtractor().extract(sourceRoot, Set.of(JavaEeEjb2JbossProfile.ID));
        assertNotNull(snapshot.javaEeEjb2Jboss);
        assertEquals(1, snapshot.javaTypes.size());
        assertEquals(1, snapshot.javaEeEjb2Jboss.ejbBeans.size());
        assertEquals(2, snapshot.javaEeEjb2Jboss.crosswalkEdges.size());

        new CodeSsotWriter().write(dbPath, snapshot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement st = conn.createStatement()) {
            ResultSet profiles =
                    st.executeQuery("SELECT profiles FROM export_run ORDER BY id DESC LIMIT 1");
            assertTrue(profiles.next());
            assertEquals(JavaEeEjb2JbossProfile.ID, profiles.getString(1));

            ResultSet beans = st.executeQuery("SELECT COUNT(*) FROM ejb_bean");
            assertTrue(beans.next());
            assertEquals(1, beans.getInt(1));

            ResultSet crosswalk = st.executeQuery("SELECT COUNT(*) FROM profile_crosswalk_edge");
            assertTrue(crosswalk.next());
            assertEquals(2, crosswalk.getInt(1));
        }
    }

    @Test
    void autoDetectEnablesProfileWhenDescriptorsPresent() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/javaee-ejb2-jboss").toAbsolutePath();
        var detected = ProfileRegistry.resolve(java.util.List.of(), sourceRoot, true);
        assertEquals(Set.of(JavaEeEjb2JbossProfile.ID), detected);
    }
}
