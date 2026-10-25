package cookbook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cookbook.DataRepository.Table;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// What index.html used to keep in the browser (IndexedDB), now shared by all devices:
//   GET    /api/data                everything: {recipes: [...], items: [...], meta: {...}}
//   PUT    /api/recipes             a list of recipes to add or replace (by id); the same for /api/items
//   DELETE /api/recipes             a list of ids to delete; the same for /api/items
//   PUT    /api/meta                {key: value, ...} to set
// The server doesn't look inside the recipes and items beyond their id; the app decides what they hold.
@RestController
@RequestMapping("/api")
public class DataController {
    private static final int MAX_ID_LENGTH = 100;

    private final DataRepository data;
    private final ObjectMapper json;

    public DataController(DataRepository data, ObjectMapper json) {
        this.data = data;
        this.json = json;
    }

    record AllData(List<JsonNode> recipes, List<JsonNode> items, Map<String, JsonNode> meta) {
    }

    static class ApiException extends RuntimeException {
        final HttpStatus status;

        ApiException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    @GetMapping("/data")
    public AllData all() {
        Map<String, JsonNode> meta = new LinkedHashMap<>();
        data.meta().forEach((key, value) -> meta.put(key, parse(value)));
        return new AllData(parseAll(data.all(Table.RECIPES)), parseAll(data.all(Table.ITEMS)), meta);
    }

    @PutMapping("/{table}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@PathVariable String table, @RequestBody List<JsonNode> objects) {
        Map<String, String> rows = new LinkedHashMap<>();
        for (JsonNode object : objects) {
            if (object == null || !object.isObject()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Every entry must be an object.");
            }
            rows.put(checkId(object.path("id").isTextual() ? object.get("id").asText() : null), object.toString());
        }
        data.putAll(table(table), rows);
    }

    @DeleteMapping("/{table}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String table, @RequestBody List<String> ids) {
        ids.forEach(DataController::checkId);
        data.deleteAll(table(table), ids);
    }

    @PutMapping("/meta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putMeta(@RequestBody Map<String, JsonNode> values) {
        Map<String, String> rows = new LinkedHashMap<>();
        values.forEach((key, value) -> rows.put(checkId(key), value == null ? "null" : value.toString()));
        data.putMeta(rows);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException e) {
        return ResponseEntity.status(e.status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "That isn't valid cookbook data."));
    }

    private static Table table(String name) {
        for (Table table : Table.values()) {
            if (table.name.equals(name)) {
                return table;
            }
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "There is no " + name + ".");
    }

    private static String checkId(String id) {
        if (id == null || id.isEmpty() || id.length() > MAX_ID_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Every entry needs an id.");
        }
        return id;
    }

    private List<JsonNode> parseAll(List<String> rows) {
        List<JsonNode> parsed = new ArrayList<>();
        rows.forEach(row -> parsed.add(parse(row)));
        return parsed;
    }

    private JsonNode parse(String text) {
        try {
            return json.readTree(text);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Broken JSON in the database: " + text, e);
        }
    }
}
