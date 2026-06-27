package com.anchor.migration.javaastssot;

import com.anchor.migration.javaastssot.core.extract.JavaAstExtractor;
import com.anchor.migration.javaastssot.core.store.JavaAstSsotStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SourceCommentSidecarTest {

    @TempDir
    Path tempDir;

    @Test
    void exportStoresRawCommentsWithoutAstBinding() throws Exception {
        Path sourceRoot = Path.of("src/test/resources/core-only").toAbsolutePath();
        Path dbPath = tempDir.resolve("comments.db");

        var snapshot = new JavaAstExtractor().extract(sourceRoot, Set.of());
        assertEquals(3, snapshot.sourceComments.size());

        new JavaAstSsotStore().write(dbPath, snapshot);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement st = conn.createStatement()) {
            ResultSet count = st.executeQuery("SELECT COUNT(*) FROM source_comment");
            assertTrue(count.next());
            assertEquals(3, count.getInt(1));

            assertComment(st, "javadoc", "Class-level javadoc for sidecar test.");
            assertComment(st, "line", "greeting method");
            assertComment(st, "block", "inline block");
        }
    }

    private static void assertComment(Statement st, String kind, String textFragment) throws Exception {
        ResultSet rs =
                st.executeQuery(
                        "SELECT kind, text FROM source_comment WHERE kind = '"
                                + kind
                                + "' AND text LIKE '%"
                                + textFragment
                                + "%'");
        assertTrue(rs.next(), "missing comment kind=" + kind + " text=" + textFragment);
        assertFalse(rs.next());
    }
}
