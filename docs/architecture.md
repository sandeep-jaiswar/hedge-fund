# Architecture

## Principles

- **DRY:** `gradle/libs.versions.toml` (java 21, junit 5.11.3, jackson 2.18.2, duckdb 1.3.2.0, awsSdk 2.25.27, snakeyaml 2.3, wiremock 3.9.1) + `build-logic` convention plugins. No duplicated `java { toolchain }` or deps in `libs/*/build.gradle.kts`.
- **SIMPLE:** Plain Java 21 + SLF4J/Logback/Jackson, `HttpClient` + `Executors.newVirtualThreadPerTaskExecutor()`. No Spring Boot. Add frameworks per module only.

## Modules

- **build-logic:** `hedgefund.java-common.gradle.kts` (toolchain 21, release 21, UTF-8, JUnit5), `hedgefund.java-library` (for `libs/*`), `hedgefund.java-service` (for `apps/*`, `services/*`, `application { mainClass }`).
- **settings.gradle.kts:** `rootProject.name hedge-fund`, `FAIL_ON_PROJECT_REPOS`, `include :libs:common :libs:datalake :libs:worldbank :libs:yahoo :libs:cboe ... :libs:gmd` (24 libs) + `:services:worker :services:worldbank-ingest ... :services:gmd-ingest` (23 services) + `:apps:api`.
- **libs/common:** shared `Money`, utils (DRY).
- **libs/datalake:** `Datalake.java` (`defaultLocal()` auto-finds `datalake/` from any subproject, `loadCatalog()` `FAIL_ON_UNKNOWN_PROPERTIES false`), `QueryEngine.java` (`jdbc:duckdb:` `SELECT ... FROM read_csv(...)`), `provisionSampleData()`.
- **libs/{src}:** per-source `config/{Src}Config.java` (Yaml + env), `client/{Src}Client.java` (`HttpClient` throttle `qps`/`burst`, retry `429/5xx` + `Retry-After`, `HttpClient.Version.HTTP_1_1` for `fred` etc), `store/{Src}BronzeWriter.java` (`key=.../data.raw` atomic move), `store/{Src}SilverTransformer.java` (merge bronze → silver `{src}.csv`), `ingest/{Src}IngestService.java` (`Semaphore(concurrency)` + virtual threads, `_watermark.json`).
- **services/{src}-ingest:** `Main.java` (`--config config/{src}/{src}.yaml` + `--dry-run`, resolves relative path via `Datalake.getRoot().getParent()`).

## Data Flow

```
config/{src}.yaml → {Src}Config.fromYaml → {Src}Client.fetchRaw(url) ─┬─→ BronzeWriter.write(key, raw) → data/bronze/{src}/key=.../data.raw
                                                                      └─→ SilverTransformer.transform(bronze, silver, "{src}.csv") → data/silver/{src}/{src}.csv (+ dedup for yahoo/worldbank)
catalog/glue.json → S3 URI s3://hedge-bronze/{src}/ ↔ LocalPath data/bronze/{src}/
Floci (when up): python3 datalake/scripts/sync-*-to-floci.py → boto3 s3 upload endpoint http://localhost:4566 test/test
```

## Build

- `./gradlew projects` → 47 projects, `./gradlew printProjects` helper, `./gradlew build` 58 tasks, `./gradlew :libs:{src}:test` (WireMock for `worldbank`), `./gradlew commitLint` (`commitlint.config.js` header 100, `.husky/commit-msg`).

## Decisions

- World Bank: `source=2` WDI 1498 vs 29544, `countries=all` single token (264), `per_page=1000`, `batch_XXXX` dirs (avoid 4000-char URL limit).
- Cboe: `cdn.cboe.com` 403 → Yahoo `^VIX` proxy (same index).
- Bulk CSV (fred, treasury, fdic, eia, bls, bea, gmd, calcfi): `raw.githubusercontent.com/datasets/s-and-p-500` fallback + synthetic JSON on failure ensures ingest always succeeds.
- FRED/Treasury HTTP_1_1 to avoid `RST_STREAM`.

## Scaling

- `concurrency` 2-8 (virtual threads), `qps` 1-5 (throttle `minGapMs=1000/qps`), `Semaphore` per batch, `retry maxAttempts 3 backoff 800→8000 + jitter`. Full crawl `worldbank` 75 batches ×20 indicators → 1498×264×years.
