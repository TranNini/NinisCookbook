package cookbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BackupTest {
    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void dataInTempDir(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("test.db"));
        registry.add("cookbook.access-code-file", () -> dataDir.resolve("access-code.txt").toString());
        registry.add("cookbook.backup-dir", () -> dataDir.resolve("backups").toString());
        registry.add("cookbook.backup-days", () -> "30");
    }

    @Autowired
    Backup backup;

    @Autowired
    JdbcClient jdbc;

    @Test
    void copiesTheDatabaseAndDeletesOldDays() throws Exception {
        jdbc.sql("DELETE FROM recipes").update();
        jdbc.sql("INSERT INTO recipes (id, data) VALUES ('r1', '{\"id\":\"r1\",\"title\":\"Pho\"}')").update();
        Path backups = dataDir.resolve("backups");
        LocalDate today = LocalDate.now();
        Path old = Files.createDirectories(backups.resolve(today.minusDays(30).toString()));
        Path kept = Files.createDirectories(backups.resolve(today.minusDays(29).toString()));
        Files.writeString(old.resolve("cookbook.db"), "old");

        Path day = backup.run();

        assertEquals(backups.resolve(today.toString()), day);
        try (Connection copy = DriverManager.getConnection("jdbc:sqlite:" + day.resolve("cookbook.db"));
             ResultSet row = copy.createStatement().executeQuery("SELECT data FROM recipes")) {
            assertTrue(row.next());
            assertEquals("{\"id\":\"r1\",\"title\":\"Pho\"}", row.getString("data"));
        }
        assertTrue(Files.exists(day.resolve("access-code.txt")));
        assertFalse(Files.exists(old), "older than 30 days");
        assertTrue(Files.exists(kept), "the 30th day is kept");

        // a second backup the same day replaces it
        jdbc.sql("DELETE FROM recipes").update();
        backup.run();
        try (Connection copy = DriverManager.getConnection("jdbc:sqlite:" + day.resolve("cookbook.db"));
             ResultSet count = copy.createStatement().executeQuery("SELECT count(*) FROM recipes")) {
            assertEquals(0, count.getInt(1));
        }
        assertFalse(Files.exists(day.resolve("cookbook.db.new")));
    }
}
