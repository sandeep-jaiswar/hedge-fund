# Development

## Requirements

- Java 21 Corretto `21.0.12.1`, Gradle wrapper `8.10.2` (`./gradlew`), Node `26.8.1` npm `11.19.0` for commitlint, Python `boto3` for Floci sync, Docker `29.7.2` for Floci `2.0.1`.

## Quick Start

```bash
./gradlew build          # 58 tasks, 47 projects
./gradlew test           # JUnit5 + WireMock
./gradlew projects       # list 47
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml"
./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank.yaml"
```

## Adding a New Module

**Library:**

```bash
mkdir -p libs/my/src/main/java/com/hedgefund/my
# libs/my/build.gradle.kts
plugins { id("hedgefund.java-library") }
dependencies { api(project(":libs:common")); api(libs.jackson.databind) }
# settings.gradle.kts
include(":libs:my")
```

**Service:**

```bash
mkdir -p services/my-ingest/src/main/java/com/hedgefund/my/ingest
# services/my-ingest/build.gradle.kts
plugins { id("hedgefund.java-service") }
dependencies { implementation(project(":libs:my")) }
application { mainClass.set("com.hedgefund.my.ingest.Main") }
include(":services:my-ingest")
```

Add shared versions to `gradle/libs.versions.toml` only.

## Tests

- `libs/worldbank/src/test/java/com/hedgefund/worldbank/WorldBankClientTest.java` WireMock `countries=all` + pagination `BUILD SUCCESSFUL 2/2`.
- Generic: `./gradlew :libs:yahoo:test` (add WireMock per source), `./gradlew :libs:datalake:test` (DuckDB 200 ticks).

## Commitlint

- `commitlint.config.js:1` `extends @commitlint/config-conventional`, header 100 body 150, `.husky/commit-msg:1` `npx --no -- commitlint --edit $1`, tasks `./gradlew commitLint` / `commitLintRange`, `npm run commitlint`.
- Types: `feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert`. Bypass `git commit --no-verify` (not recommended).

## Code Style

- **DRY:** version catalog + `build-logic` plugins. **SIMPLE:** Java records + `Executors.newVirtualThreadPerTaskExecutor()`, SLF4J/Logback/Jackson.
- `HttpClient` `HTTP_1_1` + `throttle()` + retry, `BronzeWriter` atomic `tmp`→`move`, `SilverTransformer` dedup, `Datalake.defaultLocal()` path resolution.

## Debugging

- `./gradlew :services:{src}-ingest:run --info` for throttle/retry logs, `floci doctor` / `docker ps` for Floci, `python3 datalake/scripts/sync-*-to-floci.py` for S3.

See [Architecture](architecture.md).
