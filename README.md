# hedge-fund Monorepo

Java 21 monorepo - DRY + SIMPLE principles.

## Structure

```
.
├── gradle/                 # wrapper + version catalog (single source of truth)
│   ├── libs.versions.toml  # all dependency versions - DRY
│   └── wrapper/
├── build-logic/            # convention plugins - DRY shared build logic
│   └── src/main/kotlin/
│       ├── hedgefund.java-common.gradle.kts   # Java 21, JUnit 5, UTF-8
│       ├── hedgefund.java-library.gradle.kts  # for libs/*
│       └── hedgefund.java-service.gradle.kts  # for apps/*, services/*
├── datalake/               # local medallion lake (Floci-compatible, no Docker)
│   ├── data/bronze|silver|gold/  # CSV/NDJSON sample (hedge-fund ticks/orders/ohlcv/positions)
│   ├── catalog/glue.json   # local Glue Data Catalog mock
│   └── scripts/provision.py|provision-floci.py
├── libs/
│   ├── common/             # shared code (Money, utils) - DRY
│   └── datalake/           # Java 21 Datalake + DuckDB Athena (libs/datalake/src/main/java/com/hedgefund/datalake/Datalake.java:1)
├── apps/
│   └── api/                # deployable app (executable jar)
└── services/
    └── worker/             # background service (executable jar)
```

## Principles

- **DRY**: version catalog + convention plugins. No duplicated `java { toolchain }` or test deps in each module.
- **SIMPLE**: No Spring Boot bloat by default. Plain Java 21 + SLF4J + Jackson. Add frameworks only when needed per module.
- **Java 21**: toolchain + `release=21`, virtual threads ready.

## Requirements

- Java 21 (Corretto 21 tested)
- No local Gradle install needed - uses wrapper (`./gradlew`)

## Quick Start

```bash
./gradlew build          # build all
./gradlew test           # run all tests
./gradlew :apps:api:run  # run API
./gradlew :services:worker:run

./gradlew projects       # list modules
./gradlew printProjects  # custom helper
```

## Adding a New Module

**Library:**
```bash
mkdir -p libs/my-lib/src/main/java/com/hedgefund/mylib
# create libs/my-lib/build.gradle.kts:
#   plugins { id("hedgefund.java-library") }
#   dependencies { implementation(project(":libs:common")) }
# add to settings.gradle.kts: include(":libs:my-lib")
```

**App/Service:**
```bash
mkdir -p apps/my-app/src/main/java/com/hedgefund/myapp
# build.gradle.kts:
#   plugins { id("hedgefund.java-service") }
#   application { mainClass.set("com.hedgefund.myapp.Main") }
# add to settings.gradle.kts: include(":apps:my-app")
```

Add shared deps to `gradle/libs.versions.toml` only - don't hardcode versions in modules.

## Build Logic

All shared Gradle config lives in `build-logic/`. Change Java version or test config there once, not in N modules.

## Commitlint (native)

Conventional Commits enforced via **commitlint + husky** (native npm, no Docker).

- Config: `commitlint.config.js:1` (`extends: @commitlint/config-conventional`)
- Hook: `.husky/commit-msg:1` → `npx --no -- commitlint --edit $1`
- Gradle tasks: `./gradlew commitLint` / `./gradlew commitLintRange`

```bash
npm install                  # install @commitlint/cli, husky
npm run commitlint           # lint origin/main..HEAD
./gradlew commitLint         # same via Gradle (verification group)

# test a message
echo "feat(api): add pricing" | npx commitlint        # passes
echo "bad message" | npx commitlint                    # fails: type-empty
```

Allowed types: `feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert` (conventional). Header max 100, body line 150 (`commitlint.config.js:8`).

Bypass only if needed: `git commit --no-verify` (not recommended).

## Datalake (local, Floci-compatible — no Docker)

Floci server (`floci/floci:latest`) is **Docker-only** (no standalone binary in 2.0.1). Per request, this repo runs **local file-based datalake** that mimics Floci's S3+Glue+Athena+Firehose without a container. When Docker is re-enabled (`floci start` on :4566), same tables work via `testcontainers-floci`.

- **Provision (no AWS/Docker):** `python3 datalake/scripts/provision.py` or `./gradlew :libs:datalake:run --args="provision"` → generates `datalake/data/**` + `catalog/glue.json:1`
- **Query (Athena → DuckDB):** `./gradlew :libs:datalake:test` or `QueryEngine.java:1` → `SELECT count(*) FROM read_csv('datalake/data/bronze/market_ticks/market_ticks.csv', header=true)` = 200 rows (verified)
- **Catalog:** `datalake/catalog/glue.json:1` mirrors `aws glue create-database/table` (hedge_bronze/silver/gold)
- **Floci mode (optional):** `floci start && eval $(floci env) && python3 datalake/scripts/provision-floci.py` → creates S3 buckets `hedge-bronze|silver|gold`, Glue DBs, Firehose `floci-firehose-results` (see `datalake/README.md:1`)

Java API:
```java
var lake = Datalake.defaultLocal(); // auto-finds repo/datalake from any subproject
lake.loadCatalog().databases(); // hedge_bronze, hedge_silver, hedge_gold
try (var qe = new QueryEngine()) {
  qe.query("SELECT symbol, avg(price) FROM read_csv('datalake/data/bronze/market_ticks/market_ticks.csv', header=true) GROUP BY symbol");
}
```

See `datalake/README.md:1` and `libs/datalake/README` for full Floci mapping.
