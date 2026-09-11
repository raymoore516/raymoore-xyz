# Survivor League

## Purpose

The Survivor League project publishes the current contestant picks from a Google Sheet. It is a small, read-only project and does not use a database schema.

## Spreadsheet contract

- Read the `Standings` and `Teams` worksheets in the spreadsheet identified by `SURVIVOR_SPREADSHEET_ID`.
- Use row 1 as the header row in both worksheets.
- Column A must have the header `Name`.
- A column with the header `Entry` contains checkbox/boolean values. Only rows where this value is `TRUE` are displayed.
- Columns named `Week N`, where `N` is an integer, contain contestant picks.
- Exclude a week from the selector when every data cell in that week column is empty.
- Include every active contestant in a displayed week, showing an empty pick when that contestant's week cell is blank.
- Order available weeks numerically and select the latest populated week initially.
- Treat the latest populated week as current. Do not expose its pick values through the API; return only whether each active contestant has made a selection. The page represents an existing current-week selection with a green check indicator. Historical weeks display their actual pick values.
- In `Teams`, use the `Team` column as the code and the `Name` column as its display value. Historical picks display that team name rather than the raw code from `Standings`.

The backend reads the current spreadsheet contents for every API request. The API sends `Cache-Control: no-store`, and the frontend also disables its fetch cache so reloading the page immediately reflects spreadsheet changes.

## Routes

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/survivor` | React page with the week selector and contestant-pick table. |
| GET | `/api/survivor` | Public JSON representation of populated weeks and active contestants. |

The JSON route uses Google Application Default Credentials with the read-only Sheets scope. For local development, `GOOGLE_APPLICATION_CREDENTIALS` points to a service-account JSON file, and the corresponding service-account email must have read access to the sheet.

The Survivor page intentionally does not display the shared site header or hamburger menu and is not listed in site navigation. Visitors access it directly at `/survivor`.

## Project structure

- Backend code lives under `xyz.raymoore.survivor`, organized by controller, service, and query DTO.
- Frontend code lives under `frontend/src/projects/survivor`.
- This project has no persistence package, Flyway migration, or PostgreSQL schema.
