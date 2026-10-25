package cookbook;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Reads and writes the recipes, items and meta tables (see schema.sql). The rows are JSON text;
// DataController checks and parses it.
@Repository
public class DataRepository {
    // The tables with an id and a JSON object
    public enum Table {
        RECIPES("recipes"), ITEMS("items");

        final String name;

        Table(String name) {
            this.name = name;
        }
    }

    private final JdbcClient jdbc;

    public DataRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> all(Table table) {
        return jdbc.sql("SELECT data FROM " + table.name).query(String.class).list();
    }

    // id -> JSON object; adds new rows and replaces existing ones
    @Transactional
    public void putAll(Table table, Map<String, String> rows) {
        rows.forEach((id, data) -> jdbc.sql("INSERT OR REPLACE INTO " + table.name + " (id, data) VALUES (:id, :data)")
                .param("id", id)
                .param("data", data)
                .update());
    }

    @Transactional
    public void deleteAll(Table table, Collection<String> ids) {
        ids.forEach(id -> jdbc.sql("DELETE FROM " + table.name + " WHERE id = :id").param("id", id).update());
    }

    // key -> JSON value
    public Map<String, String> meta() {
        Map<String, String> meta = new LinkedHashMap<>();
        jdbc.sql("SELECT key, value FROM meta").query((row, n) -> meta.put(row.getString("key"), row.getString("value"))).list();
        return meta;
    }

    @Transactional
    public void putMeta(Map<String, String> values) {
        values.forEach((key, value) -> jdbc.sql("INSERT OR REPLACE INTO meta (key, value) VALUES (:key, :value)")
                .param("key", key)
                .param("value", value)
                .update());
    }
}
