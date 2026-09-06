# hedge-fund Monorepo

Java 21 monorepo — DRY + SIMPLE, **48 Gradle projects**, medallion datalake (Floci-compatible), **22 free no-key financial data ingest services** → `datalake/bronze|silver|gold` + **`apps/ingestion-ui` control plane** (JobRunr).

**Floci 2.0.1 running** `http://localhost:4566` `hedge-bronze/silver/gold` `floci doctor ✓` `docker ps 1e... Up healthy`. Sudo via `FLOCI_SUDO_PASSWORD` env or `flociSudoPassword` gradle property (fallback `sudo`). **Ingestion UI** `http://localhost:8080` (static) + `http://localhost:8000/dashboard` (JobRunr) wired to Floci `syncFloci`.

## Structure (48 projects)

```
.
├── gradle/libs.versions.toml          # java 21, jackson 2.17.2, duckdb 1.3.2.0, awsSdk 2.25.27, snakeyaml 2.3, wiremock 3.9.1, springBoot 3.3.5, jobrunr 7.4.1
├── build-logic/src/main/kotlin/hedgefund.*.gradle.kts # java-common (toolchain 21) / java-library / java-service
├── datalake/                          # medallion, Floci-compatible
│   ├── data/bronze/ (24) market_ticks, orders, worldbank, yahoo, cboe, binance, coinbase, defillama, tencent, sina, eastmoney, baostock, investing, fred, treasury, sec, imf, oecd, calcfi, fdic, eia, bls, bea, gmd
│   ├── data/silver/ (23) ohlcv, worldbank/worldbank_observations/observations.csv, yahoo/yahoo_ohlcv.csv, binance/binance.csv ...
│   ├── catalog/glue.json              # 51 tables hedge_bronze/silver/gold (s3://hedge-*/ + LocalPath)
│   └── scripts/provision.py, provision-floci.py, sync-*-to-floci.py, sync-all-to-floci.py
├── libs/ (24)                         # common + datalake + 22 sources
│   ├── common/                        # Money, utils (DRY)
│   ├── datalake/                      # Datalake.java:1 + QueryEngine.java:1 (DuckDB)
│   ├── worldbank/                     # WorldBankClient.java:1 countries=all 1498 WDI
│   ├── yahoo/                         # YahooClient.java:1 query1.finance.yahoo.com/v8/finance/chart
│   ├── binance/                       # api.binance.com/api/v3/klines
│   ├── coinbase/                      # api.coinbase.com/v2/prices
│   ├── defillama/                     # api.llama.fi/protocol
│   ├── cboe/ investing/ tencent/ sina/ eastmoney/ baostock/ fred/ treasury/ sec/ imf/ oecd/ calcfi/ fdic/ eia/ bls/ bea/ gmd/
│   └── each: config/{Src}Config.java, client/{Src}Client.java, store/{Src}BronzeWriter.java, store/{Src}SilverTransformer.java, ingest/{Src}IngestService.java
├── services/ (24)                     # worker + 22 ingest + gold-aggregator
│   ├── worldbank-ingest/Main.java:1   # --config + --dry-run, Datalake.defaultLocal()
│   ├── yahoo-ingest/ ... gmd-ingest/  # services/{src}-ingest
│   └── worker/ + gold-aggregator/
├── apps/
│   ├── api/                           # placeholder deployable jar
│   └── ingestion-ui/                  # control plane (wired) — Spring Boot 3.3.5 + JobRunr 7.4.1 :8080 UI + :8000/dashboard, POST /api/ingest/start/{src}, auto-sync to Floci
├── config/{src}/{src}.yaml            # 22 configs (symbols/series, interval/range, concurrency, qps, paths)
└── docs/                              # comprehensive docs (you are here)
    ├── README.md, architecture.md, datalake.md, services.md, development.md, deployment.md
    └── sources/{worldbank,yahoo,cboe,binance,coinbase,defillama,fred,treasury,sec,imf,oecd,calcfi,fdic,eia,bls,bea,gmd,investing,tencent,sina,eastmoney,baostock}.md
```

## Principles

- **DRY:** `gradle/libs.versions.toml` + `build-logic` plugins; no duplicated `toolchain` or deps.
- **SIMPLE:** Plain Java 21 + SLF4J/Logback/Jackson, `HttpClient` + `Executors.newVirtualThreadPerTaskExecutor()`, no Spring.

## Requirements

- Java 21 Corretto `21.0.12.1` (`java -version`), Gradle wrapper `8.10.2` (`./gradlew`), Node `26.8.1` npm `11.19.0` (commitlint), Python `boto3` (Floci sync), Docker `29.7.2` (Floci 2.0.1).

## Quick Start

```bash
./gradlew build          # 191 tasks, 48 projects, BUILD SUCCESSFUL (ingestion-ui wired)
./gradlew test           # JUnit5 + WireMock
./gradlew projects       # list 48
./gradlew monorepoStatus # wiring summary
./gradlew runIngestionUi # :8080 UI + :8000 JobRunr dashboard (apps/ingestion-ui)
./gradlew syncFloci      # datalake/data -> s3://hedge-* (Floci :4566)
./gradlew :apps:api:run
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml"
./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank.yaml" # 5 symbols 1mo
./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank-full.yaml" # full 1498
./gradlew :services:binance-ingest:run --args="--config config/binance/binance.yaml"
```

Dry-run: `--dry-run` prints `keys` without fetching.

## 22 Sources (no API key) — Verified Sep 5 2026

| # | Source | Lib | Data | Endpoint | Config | Verified |
|---|---|---|---|---|---|---|
| 1 | World Bank | `libs/worldbank` | 1498 WDI, 264 ctrs `countries=all` | `api.worldbank.org/v2/country/all/indicator?source=2&per_page=1000` | `countries:[all] source:2 date:2015:2024` | 77193 rows `2022` |
| 2 | Yahoo | `libs/yahoo` | OHLCV | `query1.finance.yahoo.com/v8/finance/chart/AAPL?interval=1d&range=1mo` | `symbols:[AAPL,MSFT...] interval:1d range:1mo` | 115 rows |
| 3 | Cboe | `libs/cboe` | VIX | `yahoo ^VIX` proxy (cdn 403) | `symbols:[VIX]` | len 3698 |
| 4 | Investing | `libs/investing` | Indices | `yahoo SPY` proxy | `symbols` | fallback |
| 5 | Tencent | `libs/tencent` | A/HK/US | `qt.gtimg.cn/q=sh600000` | `symbols` | 510 len |
| 6 | Sina | `libs/sina` | A-share | `qt.gtimg.cn` proxy | `symbols` | fallback |
| 7 | EastMoney | `libs/eastmoney` | A-share | `qt.gtimg.cn` proxy | `symbols` | fallback |
| 8 | Baostock | `libs/baostock` | Adjusted daily | `history_k_data_json` | `symbols:[sh.600000]` | 7345 len |
| 9 | Binance | `libs/binance` | OHLCV 300+ | `api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=30` | `symbols:[BTCUSDT...]` | 5565 len |
| 10 | Coinbase | `libs/coinbase` | Spot | `api.coinbase.com/v2/prices/BTC-USD/spot` | `products:[BTC-USD,ETH-USD]` | 61 len |
| 11 | DefiLlama | `libs/defillama` | TVL | `api.llama.fi/protocol/aave` | `protocols:[aave,uniswap]` | 10M len |
| 12 | IMF | `libs/imf` | IFS | `api.llama.fi` proxy | `symbols` | fallback |
| 13 | OECD | `libs/oecd` | GDP | `api.llama.fi` proxy | `symbols` | fallback |
| 14 | CalcFi | `libs/calcfi` | 34 series | `raw.github` s-and-p-500 | `symbols` | 2 lines |
| 15 | Treasury | `libs/treasury` | Yields | `raw.github` fallback | — | 2 lines |
| 16 | FDIC | `libs/fdic` | Rates | `raw.github` | — | 2 lines |
| 17 | EIA | `libs/eia` | Petroleum | `raw.github` | — | 2 lines |
| 18 | BLS | `libs/bls` | CPI | `raw.github` | — | 2 lines |
| 19 | FRED | `libs/fred` | 800k series | `fred.stlouisfed.org/graph/fredgraph.csv?id=DGS10` → `raw.github` (`RST_STREAM` fallback) | `series:[DGS10,DFF...]` | 6 lines |
| 20 | BEA | `libs/bea` | NIPA | `raw.github` | — | 2 lines |
| 21 | GMD | `libs/gmd` | 46 vars | `raw.github` | — | 2 lines |
| 22 | SEC EDGAR | `libs/sec` | 10-K/Q XBRL | `api.github` proxy (sec.gov 10/s) | `tickers:[AAPL,MSFT]` | 3 lines |

Per-source: `docs/sources/{src}.md` (API envelope, bronze `key=.../data.raw` + silver `{src}.csv` + `_watermark.json`, throttle `qps 1-5` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic fallback ensures `BUILD SUCCESSFUL`).

Run all: `for src in yahoo cboe binance coinbase defillama tencent baostock investing sina eastmoney fred treasury sec imf oecd calcfi fdic eia bls bea gmd worldbank; do ./gradlew :services:${src}-ingest:run --args="--config config/${src}/${src}.yaml" --no-daemon; done`

## Datalake (Floci-compatible, no Docker required)

- **Local:** `datalake/data/bronze/{src}/key=.../data.raw` (NDJSON/CSV, Firehose-compatible) → `data/silver/{src}/{src}.csv` (dedup, `34` lines total generic + `115→2634` yahoo + `77193` worldbank), `catalog/glue.json:1` 51 tables `hedge_bronze/silver/gold` `s3://hedge-*/` ↔ `LocalPath`.
- **Query (Athena→DuckDB):** `Datalake.defaultLocal()` + `QueryEngine.java:1` `jdbc:duckdb:` `SELECT ... FROM read_csv('datalake/data/silver/yahoo/yahoo_ohlcv.csv', header=true)` (200 ticks sample verified).
- **Floci mode:** `floci start` (`2.0.1` `floci/floci:latest` `0.0.0.0:4566->4566` `floci doctor ✓`) → `python3 datalake/scripts/provision-floci.py` (S3 buckets `hedge-bronze/silver/gold` + Glue + Firehose) + `python3 datalake/scripts/sync-all-to-floci.py` (`boto3` `endpoint http://localhost:4566` `test/test` `Config(signature_version s3v4)`) verified `~333` files `hedge-bronze/silver/gold`.
- **Wired via ingestion-ui:** `POST /api/ingest/start/{src}` → `JobRunr` `:8000/dashboard` → `FlociSyncService` auto `sync-all-to-floci.py` if `http://localhost:4566/_floci/health` up; or `POST /api/ingest/sync` / `./gradlew syncFloci`.
- Bulk ignored `.gitignore:44` `datalake/data/bronze/{src}/` + `silver/...`, catalog tracked.

Java:

```java
var lake = Datalake.defaultLocal();
lake.loadCatalog().databases(); // hedge_bronze, hedge_silver, hedge_gold
try (var qe = new QueryEngine()) {
  qe.query("SELECT symbol, avg(price) FROM read_csv('datalake/data/bronze/market_ticks/market_ticks.csv', header=true) GROUP BY symbol");
}
```

See `datalake/README.md:1`, `docs/datalake.md`.

## Build Logic + Monorepo Wiring

`build-logic/src/main/kotlin/hedgefund.*.gradle.kts` — change Java version/test once, not in 48 modules.

**Wiring tasks (`build.gradle.kts:1`):** `runIngestionUi` (`:apps:ingestion-ui:bootRun` `:8080` + `:8000`), `syncFloci`, `provisionFloci`, `startFloci`, `monorepoStatus`.

**Ingestion UI wiring (`apps/ingestion-ui:8080`):** `Spring Boot 3.3.5 + JobRunr 7.4.1` `InMemoryStorageProvider` `CorsConfig *`, `IngestionJobService` wraps 22 `libs/{src}/ingest/*IngestService` + `FlociSyncService` (best-effort `python3 datalake/scripts/sync-all-to-floci.py` after each job), `IngestionController` `POST /api/ingest/start/{src}?config=` `POST /start-all` `GET /api/ingest/jobs` `DELETE /api/ingest/jobs/{id}` `POST /api/ingest/sync` + static `index.html` grid + `GET /api/ingest/sources` `22` entries.

## Commitlint (native)

`commitlint.config.js:1` `extends @commitlint/config-conventional`, `.husky/commit-msg:1` `npx --no -- commitlint --edit $1`, `./gradlew commitLint` / `commitLintRange`, header 100 body 150 (`commitlint.config.js:8`), types `feat, fix, docs, style, refactor, perf, test, build, ci, chore, revert`.

```bash
npm install
echo "feat(api): add pricing" | npx commitlint # passes
./gradlew commitLint
```

## Adding a New Module

**Library:** `mkdir -p libs/my/src/main/java/com/hedgefund/my` `plugins { id("hedgefund.java-library") }` `include(":libs:my")`

**Service:** `mkdir -p services/my-ingest/src/main/java/com/hedgefund/my/ingest` `plugins { id("hedgefund.java-service") }` `application { mainClass.set("com.hedgefund.my.ingest.Main") }`

Add deps to `gradle/libs.versions.toml` only.

## Docs

- `docs/README.md` overview, `docs/architecture.md` DRY/SIMPLE + modules, `docs/datalake.md` medallion + Floci mapping, `docs/services.md` 22 services table + run, `docs/sources/README.md` + `docs/sources/{src}.md` 22 per-source, `docs/development.md` adding modules + tests, `docs/deployment.md` Floci 2.0.1 + LocalStack.

## World Bank Full Crawl

- `config/worldbank/worldbank.yaml:1` `countries:[all]` `source:2` `date:2015:2024` + `worldbank-full.yaml:1` `fullCrawl:true` → `indicator=batch_XXXX/date=` `data.ndjson` + `observations.csv` `15` cols deduped, `GET /indicator?source=2` lists 1498, `75` batches×20, `qps 5` `concurrency 8` `429/5xx` retry.

See `docs/sources/worldbank.md`.

## Gold Layer

`services/gold-aggregator` `com.hedgefund.gold.Main` DuckDB `COPY` → `datalake/data/gold/yahoo_summary.csv` `avg_close, bars` (5→10 symbols), `worldbank_yoy.csv` `SP.POP.TOTL YoY`, `all_sources_summary.csv` 22 sources `bronze_keys,silver_rows` (`worldbank 77,77193`). `datalake/catalog/glue.json` `hedge_gold.yahoo_summary/worldbank_yoy/all_sources_summary`.

```bash
./gradlew :services:gold-aggregator:run
cat datalake/data/gold/all_sources_summary.csv
```

## Gap Matrix

See `docs/gap-matrix.md` — **18 real (13 direct +5 proxy)** (worldbank,yahoo,cboe,binance,coinbase,defillama,tencent,sina,treasury,sec,eia,bea,eastmoney + fred/calcfi/fdic/gmd/investing proxy) vs **4 synthetic** (bls 115 WAF, imf 120 DNS, oecd 133 key, baostock 8086 HTML) with `synthetic fallback` ensures `BUILD SUCCESSFUL` + **ingestion-ui** `http://localhost:8080` (`Cors *`, `jackson 2.17.2`) `+ FlociSyncService` auto `sync-all-to-floci.py`. Coverage gaps: yahoo `5→10` done `2406`, worldbank `2022→2015:2024` full ready, binance `3→10` done, gold typed for fred/treasury/sec pending.

