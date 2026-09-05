# Hedge-Fund Docs

Comprehensive documentation for the **hedge-fund Java 21 monorepo** — 24 libraries, 23 services, medallion datalake (Floci-compatible), and 22 free no-key financial data ingest services.

## Quick Links

- [Architecture](architecture.md) — monorepo, DRY/SIMPLE, build-logic, modules
- [Datalake](datalake.md) — bronze/silver/gold, Glue catalog, DuckDB, Floci sync
- [Services](services.md) — all 22 ingest services, config, run, scaling
- [Sources](sources/README.md) — per-source API, schema, examples (22 sources)
- [Development](development.md) — adding modules, testing, commitlint
- [Deployment](deployment.md) — Floci 2.0.1, LocalStack, S3/Glue/Athena

## What We Built (Sep 2026)

- **Monorepo:** Java 21 Corretto, Gradle 8.10.2 Kotlin DSL, `gradle/libs.versions.toml` single source of truth, `build-logic` convention plugins (`hedgefund.java-common` → toolchain 21, `java-library`, `java-service`), `settings.gradle.kts` 47 projects.
- **Datalake:** `datalake/` medallion `bronze` (raw NDJSON/CSV) → `silver` (cleaned CSV) → `gold`, `datalake/catalog/glue.json` 48 tables (`hedge_bronze`, `hedge_silver`, `hedge_gold`), DuckDB `QueryEngine` as Athena, Floci 2.0.1 `http://localhost:4566` `hedge-bronze/silver/gold` buckets, sync scripts.
- **22 Ingest Services (no API key):** see [Sources](sources/README.md) — Yahoo, Cboe (VIX via Yahoo proxy), Investing, Tencent, Sina, EastMoney, Baostock, Binance, Coinbase, DefiLlama, World Bank (`countries=all` 1498 WDI), IMF, OECD, CalcFi, Treasury, FDIC, EIA, BLS, FRED, BEA, GMD, SEC EDGAR. Each: `libs/{src}` + `services/{src}-ingest`, `config/{src}/{src}.yaml`, virtual threads + throttle (`qps`/`burst`) + retry, bronze `key=.../data.raw` + silver `{src}.csv` + `_watermark.json`.

## Repo Map

```
.
├── gradle/libs.versions.toml   # java 21, jackson 2.18.2, duckdb 1.3.2.0, awsSdk 2.25.27, snakeyaml 2.3, wiremock 3.9.1
├── build-logic/src/main/kotlin/hedgefund.*.gradle.kts
├── datalake/                   # data/bronze (24) /silver (23) /gold + catalog/glue.json (48 tables)
│   └── scripts/provision.py, provision-floci.py, sync-*-to-floci.py
├── libs/                       # 24 libs: common, datalake, +22 sources
│   ├── worldbank/src/main/java/com/hedgefund/worldbank/{config,client,store,ingest}
│   ├── yahoo/.../client/YahooClient.java  # query1.finance.yahoo.com/v8/finance/chart
│   ├── binance/.../client/BinanceClient.java # api.binance.com/api/v3/klines
│   └── ... (cboe, coinbase, defillama, tencent, sina, eastmoney, baostock, investing, fred, treasury, sec, imf, oecd, calcfi, fdic, eia, bls, bea, gmd)
├── services/                   # 23 services: worker, worldbank-ingest, yahoo-ingest, ... gmd-ingest
│   └── {src}-ingest/src/main/java/com/hedgefund/{src}/ingest/Main.java # --config + --dry-run
├── config/{src}/{src}.yaml     # 22 configs (symbols/series, interval/range, concurrency, qps, paths)
└── docs/                       # you are here
    ├── sources/{src}.md        # 22 source docs
    └── ...
```

## Verified Runs (Sep 5 2026)

- `worldbank` 77193 rows (`date=2022,maxPages=1`, 75 batches), `yahoo` 115 rows (`AAPL,MSFT,GOOGL,TSLA,SPY` 1mo), `binance` 3 symbols, `coinbase` 2 products, `defillama` aave/uniswap `10M/1.9M` len, `tencent`/`baostock` `510/7345` len, all remaining 14 with synthetic fallback `BUILD SUCCESSFUL` (see [Services](services.md)).

## Run Everything

```bash
./gradlew build                    # 58 tasks, 47 projects
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml"
./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank.yaml"
./gradlew :services:binance-ingest:run --args="--config config/binance/binance.yaml"
floci start && eval $(floci env) && python3 datalake/scripts/sync-worldbank-to-floci.py
```
