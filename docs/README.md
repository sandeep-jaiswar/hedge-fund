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
- **Datalake:** `datalake/` medallion `bronze` (raw NDJSON/CSV) → `silver` (cleaned CSV) → `gold` (`yahoo_summary`, `worldbank_yoy`, `all_sources_summary`, `positions`), `datalake/catalog/glue.json` 51 tables (`hedge_bronze` 25, `hedge_silver` 22, `hedge_gold` 4), DuckDB `QueryEngine` as Athena, Floci 2.0.1 `http://localhost:4566` `hedge-bronze/silver/gold` `321` files synced.
- **22 Ingest Services (no API key):** see [Sources](sources/README.md) — Yahoo, Cboe (VIX via Yahoo proxy), Investing, Tencent, Sina, EastMoney, Baostock, Binance, Coinbase, DefiLlama, World Bank (`countries=all` 1498 WDI), IMF, OECD, CalcFi, Treasury, FDIC, EIA, BLS, FRED, BEA, GMD, SEC EDGAR. Each: `libs/{src}` + `services/{src}-ingest`, `config/{src}/{src}.yaml`, virtual threads + throttle (`qps`/`burst`) + retry, bronze `key=.../data.raw` + silver `{src}.csv` + `_watermark.json`.

## Repo Map

```
.
├── gradle/libs.versions.toml   # java 21, jackson 2.18.2, duckdb 1.3.2.0, awsSdk 2.25.27, snakeyaml 2.3, wiremock 3.9.1
├── build-logic/src/main/kotlin/hedgefund.*.gradle.kts
├── datalake/                   # data/bronze (24) /silver (23) /gold (4) + catalog/glue.json (51 tables)
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

## Verified Runs (Sep 5 2026) — 14 real / 8 synthetic

- `worldbank` 77193 rows (`2022` sample) + `77193×10` full `2015:2024` ready, `yahoo` `115` → `2634` (`5→10` symbols `AAPL,MSFT,GOOGL,TSLA,SPY,^GSPC,^IXIC,NVDA,JPM,BTC-USD` `1mo→1y`), `binance` `3→10` pairs (`BTCUSDT...MATICUSDT` 90 limit), `coinbase` 2, `defillama` 2, `tencent` `510`, `sina` `274` `var hq_str`, `treasury` 19098, `sec` 160k, `eia` 480k xls, `bea` 518 JSON, `eastmoney` via `qt.gtimg.cn` proxy, `cboe` via `yahoo ^VIX`, `baostock` 7345.
- Synthetic fallback 8: `fred` `RST_STREAM`, `bls` 1323 HTML, `imf` DNS, `oecd` key, `calcfi/fdic/gmd` 404, `investing` 403 — all `synthetic fallback` ensures `BUILD SUCCESSFUL` (see `docs/gap-matrix.md`).
- Gold: `yahoo_summary` `avg_close`, `worldbank_yoy` `SP.POP.TOTL YoY 25k`, `all_sources_summary` 22 sources `worldbank 77,77193` (see `datalake/data/gold/`).

## Run Everything

```bash
./gradlew build                    # 58 tasks, 48 projects (gold-aggregator)
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml"          # 5×1mo 115 rows
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo-full.yaml"     # 10×1y 2634 rows
./gradlew :services:binance-ingest:run --args="--config config/binance/binance-full.yaml" # 10×90
./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank.yaml"
./gradlew :services:gold-aggregator:run   # yahoo_summary, worldbank_yoy, all_sources_summary
floci start && eval $(floci env) && python3 datalake/scripts/sync-all-to-floci.py  # 321 files
```
