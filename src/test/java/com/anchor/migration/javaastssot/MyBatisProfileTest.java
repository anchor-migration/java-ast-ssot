package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.core.extract.JavaAstExtractor;
import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import com.anchor.migration.javaastssot.crosswalk.MappingRoles;
import com.anchor.migration.javaastssot.profile.ProfileRegistry;
import com.anchor.migration.javaastssot.profile.mybatis.MyBatisProfile;
import com.anchor.migration.javaastssot.profile.mybatis.MyBatisSnapshot;
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

class MyBatisProfileTest {

    @TempDir
    Path tempDir;

    @Test
    void exportWithProfileWritesNamespacedTables() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/mybatis").toAbsolutePath();
        Path dbPath = tempDir.resolve("mybatis.db");

        var snapshot = new JavaAstExtractor().extract(sourceRoot, Set.of(MyBatisProfile.ID));
        MyBatisSnapshot data = snapshot.requireProfile(MyBatisProfile.ID, MyBatisSnapshot.class);
        assertEquals(2, snapshot.javaTypes.size());
        assertEquals(2, data.resultMaps.size());
        assertEquals(3, data.resultFields.size());
        assertEquals(2, data.statements.size());

        var accountMap =
                data.resultMaps.stream()
                        .filter(m -> "accountMap".equals(m.resultMapId()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(MappingRoles.PERSISTENT_ENTITY, accountMap.mappingRole());
        assertEquals("ACCOUNT", accountMap.tableName());
        assertEquals("com.example.Account", accountMap.typeStableId());

        var summaryMap =
                data.resultMaps.stream()
                        .filter(m -> "summaryMap".equals(m.resultMapId()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(MappingRoles.READ_MODEL, summaryMap.mappingRole());
        assertEquals("listSummaries", summaryMap.statementId());

        new JavaAstSsotStore().write(dbPath, snapshot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement st = conn.createStatement()) {
            ResultSet profiles =
                    st.executeQuery("SELECT profiles FROM export_run ORDER BY id DESC LIMIT 1");
            assertTrue(profiles.next());
            assertEquals(MyBatisProfile.ID, profiles.getString(1));

            ResultSet maps = st.executeQuery("SELECT COUNT(*) FROM mybatis_result_map");
            assertTrue(maps.next());
            assertEquals(2, maps.getInt(1));

            ResultSet joinStatements =
                    st.executeQuery("SELECT COUNT(*) FROM mybatis_statement WHERE join_query = 1");
            assertTrue(joinStatements.next());
            assertEquals(1, joinStatements.getInt(1));
        }
    }

    @Test
    void autoDetectEnablesProfileWhenMapperXmlPresent() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/mybatis").toAbsolutePath();
        var detected = ProfileRegistry.resolve(List.of(), sourceRoot, true);
        assertEquals(Set.of(MyBatisProfile.ID), detected);
    }
}
