# Nini's Cookbook

Recipes with photos, ingredients and steps, plus a shopping list. Plain HTML, CSS and JavaScript in the browser,
a small Spring Boot server (Java 17, Maven) with a SQLite database behind it.

## Use it

The app runs on the NAS: open https://cookbook.kulose.de on any device. Each device asks for the access code once.
All devices share the same recipes and shopping list. On the iPhone, tap Share → "Add to Home Screen" to get an app icon.

## Run locally

    mvn spring-boot:run

or run `cookbook.CookbookApp` in IntelliJ, then open http://localhost:8082. This Mac needs no access code.

## Data

- `cookbook.db`: SQLite database (see `src/main/resources/schema.sql`). Every recipe and shopping list item is stored
  as the JSON object the app works with; photos are center-cropped to 720×720 JPEG and kept as data URLs inside the recipe.
- `access-code.txt`: the code other devices enter once
- `backups/<date>/cookbook.db`: a copy from every night at 03:30, kept 30 days

Locally they are created in the folder the app is started from (git-ignored).

Backup file (Backup tab): `{ app, version, exportedAt, recipes, shoppingList }`. Restore merges recipes by id;
when both sides have a recipe, the one with the newer `updatedAt` wins. Nothing is deleted. The reminder shows when the
last backup file (or, if none, the oldest recipe) is older than 30 days.

## Project structure

| File | What it does |
|---|---|
| `CookbookApp.java` | starts the app |
| `DataController.java`, `DataRepository.java` | REST API under `/api`: load everything, save and delete recipes, items and settings |
| `Backup.java` | the nightly copy of the database |
| `AccessCode.java`, `AccessFilter.java`, `LoginController.java` | the access code login |
| `Dockerfile` | the image for the NAS |
| `static/index.html` | the whole UI |
| `static/sw.js` | offline cache: the app and the last loaded recipes can be read without a connection. Bump `VERSION` when a file changes |
| `static/login.html` | the access code page |
| `static/manifest.webmanifest`, icons | home screen app, browser tab, splash screen |
