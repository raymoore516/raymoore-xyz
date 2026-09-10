# raymoore.xyz

A personal website project, starting with **Madison SC**, an NFL pick’em league tracker for weekly picks, results, and cumulative standings. This project will deprecate the existing Madison SC website.

The project is also a learning exercise in Spring Boot, React, TypeScript, and AI-assisted development.

## Status

The database foundation is implemented: local PostgreSQL through Docker Compose, Flyway migrations, and Spring Data JDBC domain records and repositories for contestants and picks. The Spring Boot application serves public latest-week, annual-picks, and weekly-picks endpoints plus a secret-protected administrator pick-submission endpoint. The React/TypeScript frontend has shared navigation, redirects `/madisonsc` to the latest populated week, and renders responsive annual and weekly standings views.

## Developer prerequisites (macOS / Homebrew)

Install [Homebrew](https://brew.sh/) first if `brew --version` does not work. For the full project, install these tools; skip anything you already have at the required version:

```sh
brew install git node@24 openjdk@25
brew install --cask docker-desktop
```

| Tool | Why it is needed |
| --- | --- |
| Git | Clone the repository and manage source changes. An existing Git installation is fine. |
| [Node.js 24](https://formulae.brew.sh/formula/node%4024) | Runs npm, Vite, and the frontend build tools. |
| [OpenJDK 25](https://formulae.brew.sh/formula/openjdk%4025) | Compiles and runs the Java/Spring Boot backend. An existing JDK 25 is fine. |
| [Docker Desktop](https://formulae.brew.sh/cask/docker-desktop) | Runs local PostgreSQL; includes Docker Engine, the Docker CLI, and Docker Compose. Open Docker Desktop once and finish its setup. |

Optional editors used in this project's workflow:

```sh
brew install --cask intellij-idea visual-studio-code
```

[IntelliJ IDEA](https://formulae.brew.sh/cask/intellij-idea) is used for Java; [VS Code](https://formulae.brew.sh/cask/visual-studio-code) is used for the frontend. Keep your existing editors if already installed.

The versioned Node and Java formulas need shell configuration. Add these lines once to `~/.zshrc` (adjust existing Node/Java settings rather than adding duplicates):

```sh
export JAVA_HOME="$(brew --prefix openjdk@25)/libexec/openjdk.jdk/Contents/Home"
export PATH="$(brew --prefix node@24)/bin:$JAVA_HOME/bin:$PATH"
```

Reload your shell configuration and check the installations:

```sh
source ~/.zshrc
git --version
node --version
npm --version
java --version
javac --version
docker --version
docker compose version
```

Expect Node **24.x** and Java **25**. In IntelliJ, select JDK 25; if it is not automatically listed, add the JDK directory printed by `echo "$JAVA_HOME"` as a local SDK.

There is no separate `brew install` needed for Maven, PostgreSQL, React, TypeScript, Vite, Spring Boot, or Flyway. The checked-in Maven Wrapper downloads Maven **3.9.11** and resolves backend dependencies. Node includes npm, `npm ci` installs frontend dependencies, and Docker Compose downloads PostgreSQL **15.19**. For frontend-only work, Node and an editor are sufficient.

## Local developer setup: frontend on localhost:5173

The frontend was verified with Node **24.20.0**, also recorded in `frontend/.nvmrc` for developers using nvm. Node runs the frontend development tools; React runs in your browser.

From the repository root:

```sh
cd frontend
node --version
npm --version
npm ci
npm run dev
```

`npm ci` installs the exact dependencies recorded in `package-lock.json` into `node_modules/`. Run it on initial setup or after pulling dependency changes. Think of `package.json` as the frontend's build/dependency manifest, similar in purpose to Maven's `pom.xml`; its `scripts` section defines the `npm run ...` commands. Commit `package-lock.json`, but not `node_modules/` or generated `dist/` files. To intentionally change dependencies, use `npm install --save-exact <package>` (or add `--save-dev` for build tools) and commit the updated manifest and lockfile together.

Open **http://localhost:5173/** in your browser. The page displays **Hello World** and **This site is a constant work in progress...**. It needs no Spring process, Docker, PostgreSQL, or `.env` configuration. Leave the terminal running while developing and use **Ctrl+C** to stop it. Run `npm run dev` again for subsequent sessions.

**Vite** is the frontend development server and build tool. Saving a source file updates the page through React Fast Refresh, usually without a manual reload. The development port is fixed at **5173** with `strictPort: true`; if another process owns it, stop that process instead of looking for a silently changed port. The configuration lives in `frontend/vite.config.ts`. See [Vite's getting-started guide](https://vite.dev/guide/).

### Understanding the frontend files

| File under `frontend/` | Role |
| --- | --- |
| `index.html` | Browser HTML document containing the `root` element where React renders. |
| `src/main.tsx` | Starts React, mounts `App` into that element, and imports the CSS. |
| `src/App.tsx` | Top-level component; selects the current page and renders shared navigation. |
| `src/app/pages/HomePage.tsx` | Global home page returning the heading and paragraph. Edit the text here. |
| `src/projects/madisonsc/pages/LatestWeekPage.tsx` | Madison SC landing page that finds and navigates to the latest populated week, or displays the empty state. |
| `src/projects/madisonsc/pages/YearlyPicksPage.tsx` | Compact 18-week annual standings and picks view. |
| `src/projects/madisonsc/pages/WeeklyPicksPage.tsx` | Weekly standings view with breadcrumb navigation and responsive contestant/pick cards. |
| `src/projects/madisonsc/api.ts` | Fetch calls for Madison SC pick-query endpoints. |
| `src/projects/madisonsc/types.ts` | TypeScript representations of Madison SC pick-query responses. |
| `src/app/styles.css` | Shared CSS, including the responsive slide-out navigation. |
| `tsconfig.json` | Enables strict TypeScript checking. |
| `vite.config.ts` | Configures React support and the local server ports. |

A **React component** is a function describing a piece of the UI. **JSX** is the HTML-like syntax returned by that function; React turns it into browser elements. A `.tsx` file is TypeScript that can contain JSX. **TypeScript** adds type checking to JavaScript during development; the browser receives JavaScript after the build. React Router handles navigation between `/`, `/madisonsc`, and weekly Madison SC URLs. The shared header identifies the current page, and its hamburger button opens the navigation drawer on desktop and mobile. The global home page itself still has no state or API calls and does not need a Spring controller. See [React's TypeScript introduction](https://react.dev/learn/typescript).

### Frontend verification and production build

Run these commands from `frontend/`:

```sh
npm run typecheck
npm run build
npm run preview
```

`typecheck` checks types without writing compiled files. `build` also runs that check, then produces deployable HTML, JavaScript, and CSS in `frontend/dist/`. Vite alone transpiles TypeScript without type-checking, so the separate check is intentional. `preview` serves the last build at **http://localhost:4173/**; rebuild after edits when using preview, and stop it with **Ctrl+C**. Preview is a local build check, not the production server. No frontend automated tests or lint command are configured in this initial scaffold.

npm manages only `frontend/`; Maven continues to manage the Java backend. npm is used in local development and in the Docker build to install dependencies and build the static frontend. The Docker build packages those files into Spring Boot, so the running Java application does not need Node or npm. The backend serves public Madison SC JSON under `/api/madisonsc`, and Vite proxies `/api` from port 5173 to Spring Boot on port 8080 during development. The home page itself continues to need no database calls.

## Local developer setup: PostgreSQL on localhost:5432

Complete the Java and Docker installations in the prerequisites above, then start Docker Desktop. The backend uses Spring Boot 4.1.1 and the included Maven Wrapper.

PostgreSQL runs in a Docker container. You do not need a separate Postgres server installed on your Mac. Run the following from the repository root (`raymoore-xyz/`, where `compose.yaml` lives) in a bash/zsh terminal.

1. On first setup, create a file named `.env` in the repository root, alongside `compose.yaml`, with the following example values. If `.env` already exists, keep your current settings:

   ```dotenv
   DB_NAME=raymoorexyz
   DB_USER=local-dev-user
   DB_PASSWORD='local-dev-password'
   DB_PORT=5432
   API_SECRET='local-dev-api-secret'
   ```

   Set `DB_PASSWORD` and `API_SECRET` to your chosen local-only values. Keep them single-quoted so literal `$` characters are preserved; choose values without single quotes or backslashes for use with both Compose and the shell. `.env` is ignored by Git. `API_SECRET` protects administrator write requests and is not exposed to the React application. All values shown above are required: configuration files intentionally provide no inline environment-variable defaults, so a missing value fails fast instead of silently selecting a fallback.

   These example settings create database `raymoorexyz`, user `local-dev-user`, and expose Postgres only at `127.0.0.1:5432`. Change `DB_PORT` if port 5432 is already occupied.

2. Start PostgreSQL 15.19 and wait for it to be ready:

   ```sh
   docker compose up -d --wait postgres
   docker compose ps
   ```

   Compose reads the root `.env` automatically. The PostgreSQL image requires a nonempty password when initializing an empty data directory. Its first startup creates the database and user; Flyway creates the application tables when Java starts.

   `-d` runs the container in the background; `--wait` waits for the configured health check to pass. Look for `healthy` and a port mapping of `127.0.0.1:5432->5432/tcp` in `docker compose ps`. Your local database is then available at `localhost:5432` (or explicitly `127.0.0.1:5432`). This is a PostgreSQL connection endpoint; connect with Java or a database client. [Compose command reference](https://docs.docker.com/reference/cli/docker/compose/up/)

3. Export the same settings for Java, then run the application:

   ```sh
   set -a
   source .env
   set +a
   ./backend/mvnw -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
   ```

   Spring Boot does not automatically read Compose's `.env` file. `set -a` exports the variables loaded by `source` so the Java process inherits them. Repeat these commands in a new terminal or after editing `.env`. Exported shell values override Compose's `.env` values, so reload the file before restarting either process after a change.

   On startup, Flyway applies `backend/src/main/resources/db/migration/V1__create_madisonsc.sql`. It establishes `madisonsc.contestant`, `madisonsc.pick`, and the pick indexes. UUIDv4 IDs use PostgreSQL's built-in `pg_catalog.gen_random_uuid()`; no extension is required. Migration history lives in `public.flyway_schema_history`.

   `App.java` starts the Spring application and its web server on port 8080. Migrations intentionally contain no sample contestants; add local development data explicitly when needed.

4. Open a SQL session to inspect the database or add a contestant:

   ```sh
   docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
   ```

   ```sql
   SELECT * FROM madisonsc.contestant;
   -- Optional local example:
   INSERT INTO madisonsc.contestant (name) VALUES ('Example Contestant');
   SELECT installed_rank, version, description, success
   FROM public.flyway_schema_history ORDER BY installed_rank;
   ```

   Exit psql with `\q`. Run the Java command again to see the inserted contestant. UUIDs and creation timestamps are generated by Postgres.

5. Submit one to five of a contestant's weekly picks with the API secret configured in `.env`:

   ```sh
   curl --request POST \
     --header 'Content-Type: application/json' \
     --header 'api-secret: local-dev-api-secret' \
     --data '{
       "contestant": "Example Contestant",
       "picks": ["GB +3 W", "CHI -2.5 L", "DET PK T", "BUF -7", "KC +1.5"]
     }' \
     http://localhost:8080/api/madisonsc/picks/13/1
   ```

   Replace the header value with your own `API_SECRET`. A successful submission returns `{"success":true}`. Each entry uses a team code, a signed spread or `PK`, and an optional `W`, `L`, or `T`; omitting the result leaves that pick pending. The endpoint inserts each submitted batch as one transaction. You may submit the week's picks incrementally, but a contestant cannot have more than five total picks for a year and week or select the same team twice.

The backend uses the following package convention within each subproject, including future subprojects:

| Package under `xyz.raymoore.<project>` | Purpose | Current Madison SC classes |
| --- | --- | --- |
| `category` | Fixed project-specific values used for validation and business logic. | `Team` |
| `domain` | Database-mapped records representing stored data. | `Contestant`, `Pick` |
| `repository` | Spring Data JDBC interfaces and JDBC-backed classes that read and write those records. | `ContestantRepository`, `PickRepository`, `PickSubmissionRepository` |
| `dto.query` | Public read API response contracts. | `LatestWeekResponse`, `YearlyPicksResponse`, `WeeklyPicksResponse` |
| `dto.submission` | Administrator pick-submission API contracts. | `PickSubmissionRequest`, `PickSubmissionResponse` |

`@SpringBootApplication` enables Spring's automatic configuration. Spring Data JDBC creates the implementation of `ContestantRepository` in `xyz.raymoore.madisonsc.repository`, which extends `ListCrudRepository<Contestant, UUID>`. Its inherited `findAll()` method generates a SELECT for all mapped columns and returns a `List<Contestant>`; no handwritten query is needed. `PickRepository` in the same package extends `ListCrudRepository<Pick, UUID>` for pick access. The `@Bean` method registers a `CommandLineRunner`: Spring supplies its repository argument and runs it after startup migrations finish.

`Contestant` and `Pick` are immutable Java records in `xyz.raymoore.madisonsc.domain`, mapped to V1 with `@Table`, `@Id`, and explicit `@Column` names where Java and SQL differ. They use `UUID`, `Instant`, and `BigDecimal` for the corresponding database types. `Pick.underdog` uses nullable `Boolean`, and `result` preserves the nullable database text. A pick references its contestant by UUID. These are database-mapped domain objects; HTTP DTOs define separate API shapes. Each record provides a static `builder()` factory and an inner `Builder` class with fluent field methods and `build()`, implemented in plain Java without Lombok. Java records provide a readable `toString()`, so console output includes field names and values. Flyway integration follows [Spring Boot's database initialization guidance](https://docs.spring.io/spring-boot/how-to/data-initialization.html).

### Where the username and password come from

`local-dev-user` is an example database username; each developer can choose their own username and local password when creating `.env`. The database name `raymoorexyz` identifies the application. These settings define the account that the container creates on its first startup. They are independent of your Mac login and production database credentials.

| Root `.env` setting | Used by `compose.yaml` as | Purpose |
| --- | --- | --- |
| `DB_USER=local-dev-user` | `POSTGRES_USER` | Creates the local PostgreSQL administrator role named `local-dev-user`. |
| `DB_PASSWORD` | `POSTGRES_PASSWORD` | Sets that role's initial password to the value you supply. |
| `DB_NAME=raymoorexyz` | `POSTGRES_DB` | Creates the local application database named `raymoorexyz`. |
| `DB_PORT=5432` | Host port mapping | Makes the container's PostgreSQL port available on your Mac. |
| `API_SECRET` | Not used by Compose | Protects administrator POST requests after `.env` is exported to Spring Boot. |

Java and database clients use the same username, password, and database to connect. For a GUI database client, use host `127.0.0.1`, port `5432`, and the values from `.env`. The corresponding JDBC URL is `jdbc:postgresql://localhost:5432/raymoorexyz` with the example settings above.

The image applies the initialization settings only to an empty data directory. Once created, the account, password, and database persist in the `postgres_data` volume. Editing `.env` later does not rename that account or database, or reset its password. [PostgreSQL image initialization reference](https://hub.docker.com/_/postgres)

### Running from IntelliJ

Open the root `raymoore-xyz/` folder so `.env` and `compose.yaml` remain visible, then right-click `backend/pom.xml` and select **Add as Maven Project**. Select JDK 25 and create a run configuration for `xyz.raymoore.App`.

In **Run → Edit Configurations**, set **Program arguments** to `--spring.profiles.active=local`. In **Environment variables**, use **Browse for .env files and scripts** to select the root `.env` by its absolute path. Apply the settings and launch that saved configuration. IntelliJ passes the file's values to Java; opening `.env` in the editor alone does not load it. See [IntelliJ environment-file configuration](https://www.jetbrains.com/help/idea/program-arguments-and-environment-variables.html).

Outside the local profile, supply `DB_URL` (a full `jdbc:postgresql://host:port/database` URL), `DB_USER`, `DB_PASSWORD`, and `API_SECRET`.

### Starting and stopping during everyday development

After the first setup, start Docker Desktop and run this from the repository root whenever you need the database:

```sh
docker compose up -d --wait postgres
docker compose ps
```

This reuses the existing database volume and `.env` file. PostgreSQL stays running when the Java application stops.

Stop the local database with `docker compose stop postgres`, or remove the container with `docker compose down`. The named volume preserves data in both cases. Avoid `docker compose down -v` unless you intend to delete the local database. Postgres applies the database/user/password initialization variables only when the volume is empty; editing `.env` does not change an existing database password. To change it, use `\password "local-dev-user"` in psql (substitute your configured user; double quotes preserve the hyphens), update `.env`, and reload the exported variables.

For connection problems, check `docker compose ps` and `docker compose logs postgres`, confirm the local profile and exported password, and check for a port conflict. If changing `DB_PORT`, restart Compose and reload `.env` before running Java.

If an error names user `"${DB_USER}"` literally, Java did not receive a usable `DB_USER` value; check the selected IntelliJ run configuration and its `.env` path. If it names your actual username but rejects the password, check that the client password matches the password stored when the database was initialized or last changed.

### Schema changes

Add future schema changes as `V2__description.sql`, `V3__description.sql`, etc. Applied migrations must remain unchanged. The archived application's `0003.sql` uses unquoted camelCase columns, which Postgres stores as `contestantid`, `pickid`, and `entrydate`; this implementation follows the new plan's snake_case columns. The production database was converted and attached to Flyway with baseline 0 before V1 ran. Automatic baselining is now disabled by using Flyway's default setting. See the [first-attachment section in PROJECT.md](PROJECT.md) for the adoption record.

During initial development, V1 was explicitly revised to use native UUID generation without adding V2. A local database that already applied the earlier V1 will fail Flyway checksum validation. Use a fresh local database, or deliberately reconcile both existing primary-key defaults and Flyway history before restarting. A checksum repair alone does not change the old defaults. This code change does not modify existing databases or remove installed extensions.

## Backend verification

Build the backend:

```sh
./backend/mvnw -f backend/pom.xml verify
```

This builds the executable JAR; no automated tests are currently included. The build requires neither Docker nor `.env`. To verify startup migration and contestant lookup manually, run the application against the local Compose database using the setup steps above.

To run the packaged application after exporting `.env`:

```sh
java -jar backend/target/raymoore-xyz-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Production deployment to Render

The root `Dockerfile` builds and serves the entire application as one Render web service:

1. The Node stage runs `npm ci` and `npm run build`, producing `frontend/dist/`.
2. The Maven stage copies that output into Spring Boot's generated `static/` classpath directory and packages the executable JAR.
3. The runtime stage contains Java and the JAR only. Spring Boot serves both the React files and `/api` from the same origin.

The initial production deployment converted and validated the existing database before attaching Flyway. The local `.temp` copies of those one-time SQL scripts are intentionally ignored by Git and are not included in the Docker build.

In Render:

1. Create a **Web Service** from the linked Git repository.
2. Choose **Docker** as the language/runtime, select the deployment branch, leave the root directory empty, and use `./Dockerfile` as the Dockerfile path.
3. Leave Build Command, Start Command, and Docker Command empty; Render uses the Dockerfile.
4. Configure `DB_URL`, `DB_USER`, `DB_PASSWORD`, and `API_SECRET`. The container reads Render's `PORT` value and defaults to port 10000 when run elsewhere.
5. Set the health check path to `/api/madisonsc/picks/latest` so readiness checks also verify database-backed reads.
6. Deploy the selected commit. Verify `/`, `/madisonsc`, direct annual and weekly URLs, the latest-week JSON endpoint, and a missing `/api` route.

The initial production startup recorded baseline 0 and applied V1 successfully. Automatic baselining has since been removed from `application.yml`, restoring Flyway's default of `false` for subsequent deployments.

## Planned architecture

A monorepo with two top-level application directories:

- **`frontend/`** — React and TypeScript, using Vite for development and builds.
- **`backend/`** — Java and Spring Boot with Spring Data JDBC, built with Maven.

PostgreSQL stores application data, Flyway manages SQL schema changes, and Docker supports local infrastructure and deployment.

Each project has its own schema within the shared `raymoorexyz` database. The planned shared hamburger navigation will link to Home and Madison SC; this first home page has no navigation.

See [PROJECT.md](PROJECT.md) for shared technologies, architecture, authentication strategy, and implementation guidance. See the [Madison SC project plan](projects/madisonsc/README.md) for league requirements, the contestant/pick schema, routes, and acceptance criteria. Future project plans belong under `projects/<project>/README.md`.
