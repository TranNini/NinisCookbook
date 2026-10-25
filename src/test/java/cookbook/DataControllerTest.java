package cookbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataControllerTest {
    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void dataInTempDir(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dataDir.resolve("test.db"));
        registry.add("cookbook.access-code-file", () -> dataDir.resolve("access-code.txt").toString());
        registry.add("cookbook.backup-dir", () -> dataDir.resolve("backups").toString());
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void empty() {
        jdbc.sql("DELETE FROM recipes").update();
        jdbc.sql("DELETE FROM items").update();
        jdbc.sql("DELETE FROM meta").update();
    }

    private void send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String json,
                      int status) throws Exception {
        mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().is(status));
    }

    @Test
    void savesReplacesAndDeletes() throws Exception {
        send(put("/api/recipes"), """
                [{"id": "a", "title": "Pho", "ingredients": [{"amount": "1", "name": "onion"}], "temp": 180},
                 {"id": "b", "title": "Banana bread", "photo": "data:image/jpeg;base64,AAAA"}]""", 204);
        send(put("/api/recipes"), "[{\"id\": \"a\", \"title\": \"Pho bo\"}]", 204);
        send(put("/api/items"), "[{\"id\": \"i1\", \"text\": \"1 onion\", \"checked\": false}]", 204);
        send(put("/api/meta"), "{\"seeded\": true, \"lastBackup\": 1761400000000}", 204);
        send(delete("/api/recipes"), "[\"b\", \"not-there\"]", 204);

        mvc.perform(get("/api/data")).andExpect(status().isOk()).andExpect(content().json("""
                {"recipes": [{"id": "a", "title": "Pho bo"}],
                 "items": [{"id": "i1", "text": "1 onion", "checked": false}],
                 "meta": {"seeded": true, "lastBackup": 1761400000000}}""", true));
    }

    @Test
    void refusesWhatIsNoCookbookData() throws Exception {
        send(put("/api/recipes"), "[{\"title\": \"no id\"}]", 400);
        send(put("/api/recipes"), "[{\"id\": 5}]", 400);
        send(put("/api/recipes"), "[\"just text\"]", 400);
        send(put("/api/recipes"), "not json", 400);
        send(put("/api/secrets"), "[{\"id\": \"x\"}]", 404);
        mvc.perform(get("/api/data")).andExpect(content().json("{\"recipes\": [], \"items\": [], \"meta\": {}}", true));
    }
}
