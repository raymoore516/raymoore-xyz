# Survivor League

## Purpose

The Survivor League project publishes the current contestant picks from a Google Sheet. It is a small, read-only project and does not use a database schema.

## Spreadsheet contract

- Read the `Standings` and `Teams` worksheets in the spreadsheet identified by `SURVIVOR_SPREADSHEET_ID`.
- Use row 1 as the header row in both worksheets.
- Column A must have the header `Name`.
- Columns `Entry` and `Buyback` contain checkbox/boolean values. Only named rows where `Entry` is `TRUE` participate in the website, including week availability and reveal decisions.
- Columns named `Week N`, where `N` is an integer, contain contestant picks.
- Exclude a week from the selector when every entrant's cell in that week column is empty.
- Include every entrant in every displayed week, including contestants eliminated in earlier weeks.
- Order available weeks numerically and select the latest populated week initially.
- In `Teams`, use the `Team` column as the code and the `Name` column as its display value. Revealed picks display that team name rather than the raw code from `Standings`.

The backend reads both worksheets in one Google Sheets request per API request, retrieving values and effective background colors together. The API sends `Cache-Control: no-store`, and the frontend also disables its fetch cache so reloading the page immediately reflects spreadsheet changes.

## Results, elimination, and visibility

- A green cell means survival; a red cell means a loss. An unformatted, white, gray, or otherwise neutral cell means pending, including a blank cell for a still-eligible entrant.
- Use the cell's effective background color, which includes conditional formatting. Resolve theme colors through the spreadsheet theme. On the Sheets 0–1 RGB scale, green must exceed both red and blue by at least `0.05` for survival; red must exceed both green and blue by at least `0.05` for a loss. Other colors remain pending. This deliberately allows different shades while ignoring near-neutral tints.
- Process all `Week N` columns in numeric order to determine elimination history. A red blank still records a loss, even if the whole column is empty and omitted from the dropdown. Pending cells do not eliminate contestants automatically.
- The first loss eliminates an entrant, except a Week 1 loss with `Buyback = TRUE`. That exception permits continued participation, but the next loss is final. A first loss in Week 2 or later cannot use a buyback.
- A week hides **all** its team names if any entrant eligible at the start of that week has a blank pick (including whitespace-only cells). Reveal as soon as every eligible entrant has entered a pick, even when result colors are still pending. Previously eliminated entrants do not block the reveal, regardless of their later blank cells or colors. A Week 1 buyback keeps the entrant eligible, so their missing pick must still block the reveal. Each week is evaluated independently; visibility does not depend on today's date or whether the week is the latest one.
- Hidden picks are redacted in the backend response. The frontend receives `picksHidden` and `selectionExists`, displaying a green SVG check circle for a submitted selection and a yellow SVG warning triangle with a dark outline and exclamation point for a missing selection. The checkmark and exclamation point share the same stroke width. The missing-pick indicator has an accessible label and tooltip: `Pick not yet submitted for this week`. Previously eliminated entrants retain their elimination label. Row result statuses remain visible.
- On the actual loss week, show the entrant's pick (or check when hidden) in a red row. This also applies to a Week 1 loss forgiven by buyback.
- On subsequent weeks, show `- Eliminated Week N -` without emojis, where `N` is the final elimination week. Use a light-gray (`#EEE`) row with dark-gray (`#555`) names and lighter-gray (`#6B6B6B`) elimination labels. Ignore later picks for eliminated entrants. Each weekly DTO contains only elimination history through that week, never a future elimination.

The Sheets format behavior is documented in [CellData / CellFormat](https://developers.google.com/workspace/sheets/api/reference/rest/v4/spreadsheets/cells).

## Table presentation

- Headers are `Name` and `Week N Pick`.
- Row backgrounds: white for pending, green for survival, red for this week's loss, and light gray for prior elimination. Prior-elimination rows use non-italic text with regular-weight dark-gray names and lighter-gray elimination labels. Other contestant names remain bold. Accessible text also identifies result status.
- In hidden weeks, eligible entrants without a submitted pick use a very pale yellow (`#FFFBEA`) row background alongside the caution indicator. This overrides the result background only for those unsubmitted rows; prior eliminations retain their gray background.
- Sort groups as survival, pending, this week's loss, then previously eliminated. Sort the first three groups alphabetically by name (case-insensitive).
- Sort prior eliminations by elimination week descending, then alphabetically by name. For example, Week 2 eliminations Ray and Ryan appear above Week 1 elimination Jordan in the Week 3 table.
- Keep the bold week dropdown, compact table text, and horizontal scrolling for narrow screens.
- The heading explains that selections are hidden until all eligible contestants have picked.

## Routes

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/survivor` | React page with the week selector and contestant-pick table. |
| GET | `/api/survivor` | Public JSON representation of populated weeks, entrants, result status, and elimination history. |

The JSON route uses Google Application Default Credentials with the read-only Sheets scope. For local development, `GOOGLE_APPLICATION_CREDENTIALS` points to a service-account JSON file, and the corresponding service-account email must have read access to the sheet.

The Survivor page intentionally does not display the shared site header or hamburger menu and is not listed in site navigation. Visitors access it directly at `/survivor`.

## Project structure

- Backend code lives under `xyz.raymoore.survivor`, organized by controller, service, and query DTO.
- `SurvivorService` fetches the sheet snapshot; `SurvivorSheetParser` calculates weekly states, reveal rules, and sorting without retaining request data.
- `WeekView.picksHidden` describes visibility. `ContestantView.status` is `PENDING`, `SURVIVAL`, `ELIMINATION`, or `ELIMINATED`; `eliminationWeek` is nullable and represents final elimination as of that view.
- Frontend code lives under `frontend/src/projects/survivor`.
- This project has no persistence package, Flyway migration, or PostgreSQL schema.

## Manual acceptance checks

- With the sandbox's Week 1 losses, Jordan (no buyback) enters the graveyard in Week 2; Ray and Tyler (buyback checked) continue.
- Ray and Ryan's Week 2 losses place them above Jordan in Week 3's graveyard. Their empty Week 3 cells do not hide Arely and Tyler's completed Week 3 picks.
- If Ray loses Week 1 but buys back, his missing Week 2 pick hides all Week 2 team names. Jordan's Week 1 elimination without buyback does not block the reveal. Entering Ray's pick reveals the week once every other eligible entrant has picked, without requiring red/green result colors.
- Missing eligible picks hide every team name in that week, regardless of result colors. A populated pick with a pending result does not block the reveal. Even a red blank for someone eligible at the start of the week continues to block that week's reveal, although it records an elimination for later weeks.
- Unchecked entrants never appear or block a reveal. Empty weeks are omitted, while every included week lists all checked entrants.
- Verify neutral and pale red/green fills, a blank pending selection, a loss with Buyback checked after Week 1, and desktop/mobile rendering.
