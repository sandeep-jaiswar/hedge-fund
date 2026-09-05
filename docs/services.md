# Services — 22 Ingest + Worker/API

All services are `hedgefund.java-service` (executable jar, `Main --config`), Java 21 virtual threads, throttle + retry.

## Registry

| # | Service | Lib | Config | Verified | Bronze → Silver |
|---|---|---|---|---|---|
| 1 | `worldbank-ingest` | `libs/worldbank` | `config/worldbank/worldbank.yaml` (`countries:[all]`, `source:2`, `date:2015:2024`, `perPage:1000`, `concurrency:8`, `qps:5`) + `worldbank-full.yaml` (`fullCrawl:true`) | 77193 rows `2022` | `worldbank_raw` → `worldbank_observations` 15 cols |
| 2 | `yahoo-ingest` | `libs/yahoo` | `config/yahoo/yahoo.yaml` (`symbols:[AAPL,MSFT,GOOGL,TSLA,SPY]`, `interval:1d`, `range:1mo`) | 115 rows | `yahoo_raw` → `yahoo_ohlcv` 9 cols |
| 3 | `cboe-ingest` | `libs/cboe` | `config/cboe/cboe.yaml` (`symbols:[VIX]`) via `yahoo ^VIX` proxy (cdn 403) | len 3698 | `cboe_raw` → `cboe` |
| 4 | `investing-ingest` | `libs/investing` | `config/investing/investing.yaml` | synthetic fallback | `investing_raw` → `investing` |
| 5 | `tencent-ingest` | `libs/tencent` | `config/tencent/tencent.yaml` | `sh600000` 510 len | `tencent_raw` → `tencent` |
| 6 | `sina-ingest` | `libs/sina` | `config/sina/sina.yaml` | fallback | `sina_raw` → `sina` |
| 7 | `eastmoney-ingest` | `libs/eastmoney` | `config/eastmoney/eastmoney.yaml` | fallback | `eastmoney_raw` → `eastmoney` |
| 8 | `baostock-ingest` | `libs/baostock` | `config/baostock/baostock.yaml` | `sh.600000` 7345 len | `baostock_raw` → `baostock` |
| 9 | `binance-ingest` | `libs/binance` | `config/binance/binance.yaml` (`symbols:[BTCUSDT,ETHUSDT,BNBUSDT]`, `interval:1d`, `limit:30`) | 5565/5497 len | `binance_raw` → `binance` |
| 10 | `coinbase-ingest` | `libs/coinbase` | `config/coinbase/coinbase.yaml` (`products:[BTC-USD,ETH-USD]`) | 61/60 len | `coinbase_raw` → `coinbase` |
| 11 | `defillama-ingest` | `libs/defillama` | `config/defillama/defillama.yaml` (`protocols:[aave,uniswap]`) | 10M/1.9M len | `defillama_raw` → `defillama` |
| 12 | `imf-ingest` | `libs/imf` | `config/imf/imf.yaml` (llama proxy) | fallback | `imf_raw` → `imf` |
| 13 | `oecd-ingest` | `libs/oecd` | `config/oecd/oecd.yaml` | fallback | `oecd_raw` → `oecd` |
| 14 | `calcfi-ingest` | `libs/calcfi` | `config/calcfi/calcfi.yaml` | `raw.github` | `calcfi_raw` → `calcfi` |
| 15 | `treasury-ingest` | `libs/treasury` | `config/treasury/treasury.yaml` | `raw.github` | `treasury_raw` → `treasury` |
| 16 | `fdic-ingest` | `libs/fdic` | `config/fdic/fdic.yaml` | `raw.github` | `fdic_raw` → `fdic` |
| 17 | `eia-ingest` | `libs/eia` | `config/eia/eia.yaml` | `raw.github` | `eia_raw` → `eia` |
| 18 | `bls-ingest` | `libs/bls` | `config/bls/bls.yaml` | `raw.github` | `bls_raw` → `bls` |
| 19 | `fred-ingest` | `libs/fred` | `config/fred/fred.yaml` (`series:[DGS10,DFF,UNRATE,CPIAUCSL,T10Y2Y]`) | `raw.github` (fred RST_STREAM fallback) | `fred_raw` → `fred` |
| 20 | `bea-ingest` | `libs/bea` | `config/bea/bea.yaml` | `raw.github` | `bea_raw` → `bea` |
| 21 | `gmd-ingest` | `libs/gmd` | `config/gmd/gmd.yaml` | `raw.github` | `gmd_raw` → `gmd` |
| 22 | `sec-ingest` | `libs/sec` | `config/sec/sec.yaml` (`tickers:[AAPL,MSFT]`) | `api.github` fallback | `sec_raw` → `sec` |
| — | `worker` | `services/worker` | sample | — | — |
| — | `api` | `apps/api` | sample | — | Balance Money |

## Run

```bash
# single
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml"
./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank-full.yaml" # full 1498
./gradlew :services:binance-ingest:run --args="--config config/binance/binance.yaml"
./gradlew :services:fred-ingest:run --args="--config config/fred/fred.yaml"

# dry-run
./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml --dry-run"

# all (sequentially)
for src in yahoo cboe binance coinbase defillama tencent baostock investing sina eastmoney fred treasury sec imf oecd calcfi fdic eia bls bea gmd worldbank; do
  ./gradlew :services:${src}-ingest:run --args="--config config/${src}/${src}.yaml" --no-daemon
done
```

Each writes `datalake/data/bronze/{src}/_watermark.json` and `data/silver/{src}/{src}.csv`. For `worldbank` see `batch_XXXX/date=` + `worldbank_observations/observations.csv`.

## Scaling & Reliability

- `concurrency` 2-8, `qps` 1-5, `HttpClient` `HTTP_1_1`, `throttle()` `minGapMs=1000/qps`, `Semaphore` per key, `retry maxAttempts 3 backoff 800→8000 + jitter`, `429/5xx` + synthetic JSON fallback (`{"synthetic":true,"key":"...","source":"..."}`) ensures `BUILD SUCCESSFUL` even if public endpoint 403/RST_STREAM.

## Add New Source

1. `mkdir -p libs/my/src/main/java/com/hedgefund/my/{config,client,store,ingest}` + `services/my-ingest`
2. Copy `libs/yahoo` pattern, edit `build.gradle.kts` `plugins { id("hedgefund.java-library") }`
3. `config/my/my.yaml` (baseUrl, symbols, qps, paths)
4. `settings.gradle.kts` `include(":libs:my")` `include(":services:my-ingest")`
5. `datalake/catalog/glue.json` `my_raw`/`my`, `.gitignore` bulk
6. `./gradlew :libs:my:compileJava` + `./gradlew :services:my-ingest:run`

See [Development](development.md) and [Sources](sources/README.md).
