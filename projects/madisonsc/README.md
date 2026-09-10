# Madison SC — Project Plan

## Purpose and document status

Build an NFL pick'em league tracker for weekly picks against the spread, results, and cumulative standings as a project within the raymoore.xyz personal website. It will replace the existing Madison SC website.

This is the implementation plan for AI coding agents. The database foundation is implemented with Flyway V1, mapped `Contestant` and `Pick` Java records in `xyz.raymoore.madisonsc.domain`, repositories, and Spring MVC latest-week/weekly-picks endpoints. The React frontend resolves `/madisonsc` through the latest-week API, navigates to the populated weekly route, and renders responsive standings cards or an explicit empty state. The secret-protected administrator submission API is implemented. Proposed API contracts and unresolved business rules are labeled explicitly.

The [root PROJECT.md](../../PROJECT.md) owns shared technologies, application architecture, authentication strategy, database migration conventions, and site navigation. This file owns Madison SC business requirements, its database schema, routes, UI behavior, and acceptance criteria. Read [AGENTS.md](../../AGENTS.md) for collaboration guidance and [README.md](../../README.md) for developer setup and day-to-day workflows. Keep these documents consistent as decisions are made.

## Initial scope and later work

- **Proof of concept:** public Madison SC pages using the shared desktop/mobile hamburger navigation, incremental administrator pick submissions using the `api-secret` HTTP header, Postgres persistence, and Flyway SQL migrations.
- **Manual administration:** Ray is the sole administrator. He creates contestants with SQL before submitting their picks, can backfill historical weeks at any time, and records or corrects results with SQL when needed. Do not build contestant-management or result-editing APIs/screens.
- **Later:** a Picks Submission page protected by Spring Security and Google OAuth 2.0 / OpenID Connect. JWT/session design is deferred and must not block the proof of concept.
- Do not add contestant self-service, multi-administrator coordination, submission deadlines, automatic grading, or edit workflows to the initial scope.

## Madison SC domain and database

### Core model

Use lowercase `snake_case` for database identifiers, such as `pick_id`, `contestant_id`, and `entry_date`. Tables remain `madisonsc.contestant` and `madisonsc.pick`. This is the schema contract for both local development and production. Java properties may use normal `camelCase`; map them with Spring Data JDBC conventions or explicit `@Column` annotations as appropriate.

| Table | Column | Type and baseline constraints | Meaning |
| --- | --- | --- | --- |
| `madisonsc.contestant` | `contestant_id` | UUID primary key, database-generated default | Stable contestant identity. |
| | `entry_date` | `timestamptz NOT NULL DEFAULT now()` | Creation time. |
| | `name` | `text NOT NULL UNIQUE` | Display name. |
| `madisonsc.pick` | `pick_id` | UUID primary key, database-generated default | Stable pick identity. |
| | `entry_date` | `timestamptz NOT NULL DEFAULT now()` | Creation time. |
| | `contestant_id` | UUID, required foreign key to contestant | Pick owner. |
| | `year` | `int NOT NULL` | Competition year identifier. |
| | `week` | `int NOT NULL` | Week within that competition year. |
| | `team` | `text NOT NULL` | Team code, e.g. `GB`, `CHI`, `JAC`. |
| | `underdog` | Nullable boolean | Positive-spread indicator; NULL is valid for a zero-line PK pick. |
| | `line` | `numeric(3,1) NOT NULL` | Spread magnitude. |
| | `result` | Nullable text | `win`, `loss`, `tie`; null means pending. |

V1 uses PostgreSQL's built-in `pg_catalog.gen_random_uuid()` for UUIDv4 primary-key defaults; no extension is required. The old scripts used unquoted camelCase columns; V1 creates snake_case columns without converting existing legacy tables. Confirm the live schema before production adoption. Existing tables also need their UUID defaults checked: `CREATE TABLE IF NOT EXISTS` does not update them.

Database invariants and mapping rules:

- A contestant has many picks across years and weeks. Keep the unique key `(contestant_id, year, week, team)`.
- V1 uses the archived migration as reference, with snake_case columns as confirmed during implementation. Its pick index names are `pick_contestant_id`, `pick_year`, `pick_week`, `pick_team`, `pick_result`, and unique `pick_unique`. The index `pick_contestant_id` reflects the full `contestant_id` column name. Production currently has `pick_contestantid`; rename that index when reconciling the production columns before V1 runs, so V1 does not create an equivalent duplicate index. The name uniqueness constraint already creates an index, so V1 omits the redundant `contestant_name` index on fresh databases and preserves it where it already exists. Review indexes against actual queries as those are added.
- Map UUIDs to Java `UUID`, timestamps to `Instant`, and `numeric(3,1)` to `BigDecimal`. Validate nonnegative magnitude, at most one decimal place, and a maximum of `99.9`.
- Store `GB +3.5` as `team = GB`, `underdog = true`, `line = 3.5`; a negative spread uses `underdog = false`. Display zero as `PK`.
- Define valid NFL team codes in `xyz.raymoore.madisonsc.category.Team`. Submission parsing uses the enum for validation and converts the selected team to its uppercase code at the string-backed `Pick` persistence boundary. Use explicit result values and validate unknown values instead of interpreting them as pending.
- A zero line is a PK (Pick Em) game. Its `underdog` value may be NULL or false; direction is irrelevant because the UI always displays `PK` without a sign when `line = 0`. Check the line before the direction. Preserve the nullable SQL column and map it to Java `Boolean`; new PK submissions may consistently write false. A nonzero line requires a known direction; handle a malformed existing row explicitly rather than inventing a sign. No nullability change or data rewrite is needed.
- “Name normalization” means rules such as trimming spaces or treating `Ray` and `ray` as equivalent. Keep the existing case-insensitive contestant-name lookup for submissions and preserve the stored display name. The SQL uniqueness constraint is case-sensitive, so the administrator should avoid names differing only by case. Reject an ambiguous lookup with `409` instead of selecting an arbitrary row; a new normalization system or schema change is not required.
- A contestant must exist before a pick can reference its UUID. Ray creates contestants by manual SQL insert; the API never creates them implicitly. Contestants do not need Google accounts or a user-to-contestant association for this administrator-managed workflow.

There are no required matchup, schedule, or team tables in the initial model. Each pick contains its own spread and manually recorded outcome. Automated NFL score ingestion is outside the initial scope. Add tables only when a concrete feature needs them.

### Competition years and reference page

`year` is the year of the competition, not a calendar year. Year 13 is the **2026-2027 NFL season**, and Year 11 is the **2024-2025 NFL season**. Use these labels consistently in navigation and page headings; keep the competition number in URLs and database rows. With continuous annual numbering, the display mapping is season start year = competition year + 2013. Do not use that mapping to infer the active week or reject historical submissions.

The live [Year 11, Week 18 page](https://raymoore.xyz/madisonsc/picks/11/18) is a content reference: it displays week links 1–18, five picks per contestant, team logos, spreads, results, and weekly/cumulative records. Retain that information in the React design; the new shared navigation and medal indicators for the top three ranks are additional UI requirements.

### Weekly views and ranking

Confirmed rules:

- Each contestant may have **up to five picks per week**, with distinct teams as required by the unique key. Ray can submit those picks incrementally in batches of one to five. A missing or partial submission remains as-is; do not manufacture picks or losses.
- Show picks and a win-loss-tie record for the selected week, plus a cumulative record through that week.
- Rank the weekly view by **cumulative win percentage through the selected week**, highest first. Pending results do not count as completed wins, losses, or ties in the displayed records.
- Later weeks must not contribute to an earlier week's record or ranking.
- The administrator may submit/backfill any competition year and week at any time. Validate positive competition years and weeks 1–18, not calendar deadlines.
- A contestant with any picks in the selected competition year appears in its standings. Such a contestant can have an empty selected week, displayed as “No picks found.” A globally existing contestant with no picks that year need not appear; no season-roster table is needed for the initial application.
- Identical W-L-T records share the same rank. Alphabetical contestant name is a display ordering, not a competitive tiebreaker. Use UUID only as a final stable display key.

Calculate cumulative win percentage as `(wins + 0.5 × ties) / completed games`, where completed games are wins, losses, and ties; pending results are excluded. Return the value as a decimal rounded to four places using half-up rounding, and return `0.0000` when there are no completed games. Rank using the exact unrounded ratio so rounding cannot create or remove a tie. This makes a cumulative `1-1-3` record (`0.5000`) rank above `2-3-0` (`0.4000`).

Calculate competition ranks as `1, 2, 2, 4`, with tied contestants sharing a rank and the following rank skipping the tied positions. In the UI, prepend gold, silver, and bronze medal emoji to contestants ranked first, second, and third respectively; display no rank marker for fourth place or lower. Keep rank calculation separate from alphabetical display ordering; do not introduce a competitive name-based tiebreaker.

### Administrator workflow

1. Ray inserts a contestant into `madisonsc.contestant` using SQL, before submitting picks for that name.
2. Ray sends a batch of one to five picks to the secret-protected POST endpoint. He can submit multiple batches until that contestant has five picks for the week. A batch may include initial results for historical backfills or leave results pending.
3. Subsequent result entry/corrections and other data corrections are manual SQL operations. There is no result-update API or UI in the proof of concept.
4. Public readers view the updated data on the next fetch/reload. Live push updates are unnecessary.

Keep contestant data and operational pick/result changes separate from Flyway schema migrations. Manual administration does not imply bypassing foreign keys, uniqueness, or atomic insertion checks in the application.

## HTTP API and browser routes

Retain the existing administrator submission header and shorthand body for the proof of concept. Unlike the archived Javalin route, the Spring endpoint lives under `/api` because it exchanges JSON rather than rendering the React route. The public latest-week and weekly-picks APIs and administrator submission API are implemented.

| Method | Path | Purpose / intended access |
| --- | --- | --- |
| GET | `/madisonsc` | React landing page. It asks the public API for the latest year/week and displays that week's data, or an explicit empty state. |
| GET | `/api/madisonsc/picks/latest` | Public latest-week lookup, returning the maximum `year`/`week` with pick entries or an empty result. |
| GET | `/madisonsc/picks/{year}` | React annual view with compact snapshots for Weeks 1–18. |
| GET | `/api/madisonsc/picks/{year}` | Public annual view data, including each week's cumulative standings. |
| GET | `/madisonsc/picks/{year}/{week}` | React weekly view. |
| GET | `/api/madisonsc/picks/{year}/{week}` | Public weekly view data, including cumulative standings. |
| POST | `/api/madisonsc/picks/{year}/{week}` | Administrator-only submission of one to five picks using `api-secret`; JSON response. |

The JSON GET and POST intentionally use the same `/api/madisonsc/picks/{year}/{week}` resource and are distinguished by HTTP method. The React browser route remains separate under `/madisonsc`. There are no contestant CRUD, result PATCH, or `/api/auth/me` endpoints in the initial scope.

### Madison SC landing page

`GET /api/madisonsc/picks/latest` runs this query against the singular table `madisonsc.pick`:

```sql
SELECT year, week
FROM madisonsc.pick
ORDER BY year DESC, week DESC
LIMIT 1;
```

Return JSON containing the selected `year` and `week`, or `{"year": null, "week": null}` when no picks exist. The maximum competition year/week determines the selected week, not the newest insertion timestamp, current calendar date, or whether results are complete. Backfilling an older week must not move the selected week backward. The React `/madisonsc` page replaces its browser-history entry with `/madisonsc/picks/{year}/{week}` when a latest week exists; an empty result renders a clear “No picks found” state with the shared navigation. A dashboard or season-selection landing page may be added later.

### Administrator pick submission

Submission body (five distinct teams; fictional example):

```json
{
  "contestant": "Example Contestant",
  "picks": [
    "GB +3.5 W",
    "CHI -2.5 L",
    "DET PK T",
    "BUF -7",
    "KC +1.5"
  ]
}
```

Each shorthand entry contains a team code, a signed spread or `PK`, and an optional `W`, `L`, or `T`. Missing result means pending. Parse the format deliberately: reject unknown teams/results, missing signs on non-`PK` spreads, invalid decimals, extra tokens, and duplicate teams with a useful validation error. Keep the request contract; use typed values inside the service and persistence layers. A structured request DTO for a future UI is deferred.

Accept one to five picks per request, insert the submitted batch atomically, and return HTTP `200` with a JSON object `{ "success": true }`. Validate the contestant exists and count that contestant's existing picks for the selected year/week while holding the contestant lock. Reject the request with `409` if the existing count plus the batch size would exceed five. Reject duplicate teams within the batch or across earlier batches; never silently replace data. One invalid entry must leave the batch unsaved. Missing/invalid secrets return `401`.

### Weekly response

The implemented weekly response includes `year`, `week`, season label, available competition years, and ordered contestant summaries containing ID, name, rank, cumulative win percentage, cumulative record, weekly record, and that week's picks. Records use numeric fields (`wins`, `losses`, `ties`) rather than only formatted strings. Spring calculates standings and React formats and displays them.

The annual response includes `year`, season label, and all 18 week snapshots. Each snapshot uses the same cumulative-percentage ranking and contestant summary contract as the weekly response. The service loads a competition year's picks and contestants once, then derives every weekly cutoff from that shared data.

## Authentication and authorization

Follow the [site authentication strategy in PROJECT.md](../../PROJECT.md). Public Madison SC pages and JSON reads require no login. Ray is the sole administrator.

For the proof of concept, protect `POST /api/madisonsc/picks/{year}/{week}` with the required `api-secret` header backed by the required `API_SECRET` environment variable. The controller binds the header with `@RequestHeader` and validates it before processing the submission. Spring MVC rejects a missing header as a bad request, while an invalid secret returns `401`; missing application configuration prevents startup, and a configured but blank secret prevents writes. The React application never receives the secret. Local API-client submissions can use Vite's `/api` proxy or go directly to `http://localhost:8080/api/madisonsc/picks/{year}/{week}`.

A Picks Submission page belongs to the later Google-login phase. It must authorize Ray's configured identity on the backend and must never contain the shared API secret. Browser session/JWT decisions and whether to retain the separate secret-authenticated API are deferred as described in the root plan. Contestants do not need login accounts or a user-to-contestant association.

## Persistence and migration details

The `madisonsc` schema belongs to this project within the shared `raymoorexyz` database. Follow the [root database change workflow in PROJECT.md](../../PROJECT.md), including idempotent initial SQL, immutable versioned migrations, and baseline-at-zero adoption of the existing production schema.

- Keep migration files in `backend/src/main/resources/db/migration/`, starting with `V1__create_madisonsc.sql`; a subsequent agreed change could use `V2__add_pick_validation_constraints.sql`. Versions belong to the shared application migration sequence.
- Initially configure Flyway's managed schemas as `public,madisonsc`, with its history table in `public`, so existing Madison SC objects participate in the empty/nonempty schema check.
- Preserve matching existing production objects and data during initial creation. Keep sample contestants/picks in explicit local fixtures or test resources, separate from production migrations and manual operational data changes.
- Treat `Contestant` and `Pick` as separate Spring Data JDBC aggregate roots. A pick references its contestant by UUID or `AggregateReference`; do not place every historical pick in a persistence-managed contestant collection.
- Insert weekly pick batches in a Spring-managed service transaction. Fetch weekly summaries in bounded queries, avoiding one contestant lookup per row. Keep standings calculation in small, testable Java methods.
- There is one administrator and no application edit workflow: do not add version columns, conflict-resolution screens, or custom concurrency infrastructure. Keep ordinary database constraints and atomic batch writes.
- Apply Bean Validation to nested pick entries and enforce cross-field and league rules in services. Enforce the secret, one-to-five request size, and five-total-picks cap on the backend; historical backfill remains allowed.

## Frontend organization and navigation

Use the shared React/TypeScript conventions, `SiteLayout`, and `SiteNavigation` from the root plan.

- Put Madison SC pages in `frontend/src/projects/madisonsc/pages/`, API calls in `api.ts`, shared API types in `types.ts`, and project components in `frontend/src/projects/madisonsc/components/` when needed. The latest-week landing page is `LatestWeekPage.tsx`; suggested weekly components are `WeekNavigation`, `ContestantCard`, `RecordSummary`, and `PickCard`. Shared application pages and components belong under `frontend/src/app/`.
- Put backend code under `backend/src/main/java/xyz/raymoore/madisonsc/`, organized into `category`, `controller`, `service`, `domain`, `repositorye`, and `dto` as needed. Put fixed project values such as `Team` in `category`, mapped records in `domain`, and repository interfaces and classes in `repository`. Group public read contracts in `dto.query` and administrator pick-submission contracts in `dto.submission`, following the shared package and inner `Builder` conventions in `PROJECT.md`. Keep typed API calls and team/result types aligned with the backend contract.
- Define React routes for `/madisonsc`, `/madisonsc/picks/:year`, and `/madisonsc/picks/:year/:week`. Use relative API paths such as `/api/madisonsc/picks/13` and `/api/madisonsc/picks/13/1` through Vite's `/api` proxy during development.
- The shared Madison SC link points to `/madisonsc`. React calls the latest-week API, then loads the selected weekly data. A normal navigation link also works on a direct browser visit.
- Put a shared, centered breadcrumb banner above both picks views. The annual view displays `Madison SC » Year 12 (2025-2026)`, with the current year bold and neither segment linked. The weekly view adds a bold selected week and links only its year segment back to the annual view, such as `Madison SC » Year 12 (2025-2026) » Week 4`. Keep `Madison SC` and the current week non-clickable. Madison SC links use distinct blue text without underlines, including the linked year and annual week-card headers. Year selection and a separate selection dashboard are deferred.
- Show “Year 12 has been suspended in loving memory of Reyna” in a visually distinct memorial banner on every Year 12 weekly view, matching the archived site's year-specific behavior.
- Show the same Reyna memorial banner on the Year 12 annual view.
- Render the annual view as a responsive grid of 18 compact week cards. Link each week-card header to that week's detailed view. Within each card, put every contestant on one row ordered by cumulative win percentage through that week. Show the medal-prefixed name, cumulative record, weekly record, and five fixed pick cells. Fill unused cells with an empty marker; show only the team code without logos or spreads in populated cells, using green for wins, yellow for ties, red for losses, and gray for pending results. Protect the full W-L-T record values as the card narrows, allowing the other columns to compress first.
- Keep historical links such as `/madisonsc/picks/11/18` directly navigable and refreshable. Restrict the production SPA fallback to GET/HEAD UI requests; administrator submissions use the separate `/api` namespace.
- Cancel or ignore stale requests when the selected year/week changes. Provide loading, error, empty, and success states, and textual result labels alongside colors. Retain team logos, spreads, results, and weekly/cumulative records from the reference page.
- Use controlled inputs when adding the later Picks Submission page. Result-entry and contestant-management pages remain outside scope.
- Initial UI acceptance includes narrow mobile and desktop viewports, public empty states, and shared menu keyboard operation.

## Verification

Apply the shared test tooling and delivery checks in the root plan, with these Madison SC acceptance cases:

- When unit tests are explicitly requested, use JUnit for cumulative win-percentage ranking, including `1-1-3` ranking above `2-3-0`, identical records sharing rank, historical week cutoffs, and pending/no-results cases. Also cover competition-year labels, incremental pick batches, the five-total-picks cap, distinct teams, and shorthand validation.
- Use real Postgres integration tests for contestant/pick mappings, UUID and timestamp defaults, enum conversion, uniqueness, and transaction rollback. Cover idempotent schema initialization, baseline-at-zero adoption of the existing schema, and `snake_case` column mappings.
- Cover PK rendering with both NULL and false directions.
- Test administrator POST requests with valid, missing, invalid, and unconfigured secrets. Verify one invalid pick cannot leave a partially inserted batch, incremental batches can reach five total picks, a batch cannot exceed that total, and historical backfill remains allowed. Google-login tests belong to the later phase.
- Use Vitest and React Testing Library for request failures, empty weeks, medal display for the top three ranks, and integration with shared navigation.
- Verify `/madisonsc` loads through both Vite and the packaged app, requests the latest year/week through the API, renders the selected weekly data, and handles an empty database. Use test fixtures with different insertion and competition-year/week orderings; the selected week must remain data-driven as new picks arrive.
- Verify direct navigation and refresh for historical weekly routes, and confirm administrator POST requests receive JSON instead of the SPA fallback.

## Remaining decisions

- In the later Google-login phase, apply the shared browser authentication design to the Picks Submission page. This does not block secret-authenticated submissions.

Competition-year meaning, ranking by cumulative win percentage, a maximum of five picks per contestant/week through incremental administrator-managed submissions, unrestricted historical backfill, manual contestant/result maintenance, PK direction handling, use of the existing production database, idempotent SQL, and API-driven latest-week selection are settled requirements. Do not reopen them or add management interfaces as prerequisites to the proof of concept.
