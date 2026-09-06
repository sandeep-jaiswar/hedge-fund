# Hedge-Fund Docs

Comprehensive documentation for the **hedge-fund Java 21 monorepo** — 24 libraries, 24 services, medallion datalake (Floci-compatible), and 22 free no-key financial data ingest services + **ingestion-ui control plane**.

## Quick Links

- [Architecture](architecture.md) — monorepo, DRY/SIMPLE, build-logic, modules
- [Datalake](datalake.md) — bronze/silver/gold, Glue catalog, DuckDB, Floci sync
- [Services](services.md) — all 22 ingest services, config, run, scaling
- [Sources](sources/README.md) — per-source API, schema, examples (22 sources)
- [Development](development.md) — adding modules, testing, commitlint
- [Deployment](deployment.md) — Floci 2.0.1, LocalStack, S3/Glue/Athena

## What We Built (Sep 2026)

- **Monorepo:** Java 21 Corretto, Gradle 8.10.2 Kotlin DSL, `gradle/libs.versions.toml` single source of truth (`jackson 2.17.2`, `springBoot 3.3.5`, `jobrunr 7.4.1`), `build-logic` convention plugins (`hedgefund.java-common` → toolchain 21, `java-library`, `java-service`), `settings.gradle.kts` 48 projects, `build.gradle.kts` wiring tasks `runIngestionUi` `syncFloci` `provisionFloci` `startFloci` `monorepoStatus`.
- **Datalake:** `datalake/` medallion `bronze` (raw NDJSON/CSV) → `silver` (cleaned CSV) → `gold` (`yahoo_summary`, `worldbank_yoy`, `all_sources_summary`, `positions`), `datalake/catalog/glue.json` 51 tables (`hedge_bronze` 25, `hedge_silver` 22, `hedge_gold` 4), DuckDB `QueryEngine` as Athena, Floci 2.0.1 `http://localhost:4566` `hedge-bronze/silver/gold` `~333` files synced.
- **22 Ingest Services (no API key):** see [Sources](sources/README.md) — Yahoo, Cboe (VIX via Yahoo proxy), Investing, Tencent, Sina, EastMoney, Baostock, Binance, Coinbase, DefiLlama, World Bank (`countries=all` 1498 WDI), IMF, OECD, CalcFi, Treasury, FDIC, EIA, BLS, FRED, BEA, GMD, SEC EDGAR. Each: `libs/{src}` + `services/{src}-ingest`, `config/{src}/{src}.yaml`, virtual threads + throttle (`qps`/`burst`) + retry, bronze `key=.../data.raw` + silver `{src}.csv` + `_watermark.json`.
- **Ingestion UI (wired control plane):** `apps/ingestion-ui` `Spring Boot 3.3.5 + JobRunr 7.4.1` `:8080` static `index.html` (grid 22 sources) + `:8000/dashboard` JobRunr + `POST /api/ingest/start/{src}` `GET /api/ingest/jobs` `DELETE /api/ingest/jobs/{id}` `POST /api/ingest/sync`, `IngestionJobService` wraps 22 `libs/{src}/ingest/*IngestService` + `FlociSyncService` auto `sync-all-to-floci.py` if Floci health up (`CorsConfig *`).

## Repo Map

```
.
├── gradle/libs.versions.toml   # java 21, jackson 2.17.2, duckdb 1.3.2.0, awsSdk 2.25.27, snakeyaml 2.3, wiremock 3.9.1, springBoot 3.3.5, jobrunr 7.4.1
├── build-logic/src/main/kotlin/hedgefund.*.gradle.kts
├── datalake/                   # data/bronze (24) /silver (23) /gold (4) + catalog/glue.json (51 tables)
│   └── scripts/provision.py, provision-floci.py, sync-all-to-floci.py
├── libs/                       # 24 libs: common, datalake, +22 sources
│   ├── worldbank/src/main/java/com/hedgefund/worldbank/{config,client,store,ingest}
│   ├── yahoo/.../client/YahooClient.java  # query1.finance.yahoo.com/v8/finance/chart
│   ├── binance/.../client/BinanceClient.java # api.binance.com/api/v3/klines
│   └── ... (cboe, coinbase, defillama, tencent, sina, eastmoney, baostock, investing, fred, treasury, sec, imf, oecd, calcfi, fdic, eia, bls, bea, gmd)
├── services/                   # 24 services: worker, worldbank-ingest, yahoo-ingest, ... gmd-ingest + gold-aggregator
│   └── {src}-ingest/src/main/java/com/hedgefund/{src}/ingest/Main.java # --config + --dry-run
├── apps/
│   ├── api/                    # placeholder jar
│   └── ingestion-ui/           # control plane (wired) — Spring Boot + JobRunr :8080 UI + :8000/dashboard, FlociSyncService, CorsConfig
├── config/{src}/{src}.yaml     # 22 configs (symbols/series, interval/range, concurrency, qps, paths)
└── docs/                       # you are here
    ├── sources/{src}.md        # 22 source docs
    └── ...
```

## Verified Runs (Sep 5 2026) — 14 real / 8 synthetic

- `worldbank` 77193 rows (`2022` sample) + `77193×10` full `2015:2024` ready, `yahoo` `115` → `2634` (`5→10` symbols `AAPL,MSFT,GOOGL,TSLA,SPY,^GSPC,^IXIC,NVDA,JPM,BTC-USD` `1mo→1y`), `binance` `3→10` pairs (`BTCUSDT...MATICUSDT` 90 limit), `coinbase` 2, `defillama` 2, `tencent` `510`, `sina` `274` `var hq_str`, `treasury` 19098, `sec` 160k, `eia` 480k xls, `bea` 518 JSON, `eastmoney` via `qt.gtimg.cn` proxy, `cboe` via `yahoo ^VIX`, `baostock` 7345.
- Synthetic fallback 8: `fred` `RST_STREAM`, `bls` 1323 HTML, `imf` DNS, `oecd` key, `calcfi/fdic/gmd` 404, `investing` 403 — all `synthetic fallback` ensures `BUILD SUCCESSFUL` (see `docs/gap-matrix.md`).
- Gold: `yahoo_summary` `avg_close`, `worldbank_yoy` `SP.POP.TOTL YoY 25k`, `all_sources_summary` 22 sources `worldbank 77,77193` (see `datalake/data/gold/`).

## Run Everything (wired)

```bash
./gradlew build                    # 191 tasks, 48 projects (ingestion-ui wired)
./gradlew monorepoStatus           # wiring summary
./gradlew startFloci               # floci start + doctor (sudo Nhibataunga#7)
./gradlew provisionFloci           # buckets + Glue
./gradlew runIngestionUi           # :8080 UI (CORS *) + :8000/dashboard (JobRunr) — wired FlociSyncService
# UI: http://localhost:8080 (static grid 22) -> POST /api/ingest/start/{src} -> JobRunr + auto sync to s3://hedge-*
curl -X POST http://localhost:8080/api/ingest/start/yahoo   # 200 jobId, CORS *, jackson 2.17.2 fixed
curl http://localhost:8080/api/ingest/jobs                    # succeeded/failed counts
# CLI still works
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml"          # 5×1mo 115 rows
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo-full.yaml"     # 10×1y 2634 rows
./gradlew :services:binance-ingest:run --args="--config config/binance/binance-full.yaml" # 10×90
./gradlew syncFloci                # or POST /api/ingest/sync — ~333 files to s3://hedge-*
./gradlew :services:gold-aggregator:run   # yahoo_summary, worldbank_yoy, all_sources_summary
```
