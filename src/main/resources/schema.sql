-- Each recipe and shopping list item is the JSON object the app works with (see index.html), stored
-- whole. Recipe photos are data URLs inside the JSON, so this file holds everything.
CREATE TABLE IF NOT EXISTS recipes (
    id   TEXT PRIMARY KEY,
    data TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS items (
    id   TEXT PRIMARY KEY,
    data TEXT NOT NULL
);

-- Settings shared by all devices, e.g. lastBackup (when a backup file was last saved); value is JSON
CREATE TABLE IF NOT EXISTS meta (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
