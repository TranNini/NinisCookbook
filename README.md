# Nini's Cookbook

https://trannini.github.io/NinisCookbook/

Offline web app (PWA) in plain HTML, CSS and JavaScript. No build step.

Files: `index.html` (whole app), `sw.js` (offline cache), `manifest.webmanifest`, icons (home screen, browser tab, splash screen).

- Data lives on the device in IndexedDB (`ninis-cookbook` database: `recipes`, `items`, `meta`).
- Photos are center-cropped to 720×720 JPEG and stored as data URLs inside the recipe.
- Backup file: `{ app, version, exportedAt, recipes, shoppingList }`. Restore merges recipes by id;
  when both sides have a recipe, the one with the newer `updatedAt` wins. Nothing is deleted.
- Backup reminder shows when the last backup (or, if none, the oldest recipe) is older than 30 days.

## Run / install
Serve the folder over HTTPS (for example GitHub Pages), open it on the phone and use
"Add to Home Screen". Locally: `python3 -m http.server -d cookbook 8000`.
When you change files, bump `VERSION` in `sw.js` so installed copies update.

## Preview artifact
The claude.ai preview is generated from `index.html` by keeping what is between the
`HEAD-START/END` and `BODY-START/END` markers and dropping the manifest/icon links.
