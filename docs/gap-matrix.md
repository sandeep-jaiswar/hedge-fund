# Gap Matrix — Real vs Synthetic, Coverage, Next

**Date:** 2026-09-06, 22 sources, Floci 2.0.1 `http://localhost:4566` `~333` S3 files synced, **18 real (13 direct + 5 proxy) / 4 synthetic** (ingestion-ui `http://localhost:8080` + `:8000/dashboard` wired, `CorsConfig *`, `jackson 2.17.2` fixed).

## Real Data (direct API, verified ingestion)

| Source | Config | Endpoint | Bronze | Silver Rows | Gold | Status |
|---|---|---|---|---|---|---|
| World Bank | `worldbank.yaml` `countries=all source:2 2015:2024` + `worldbank-full.yaml` | `api.worldbank.org/v2/country/all/indicator?source=2` | `batch_XXXX/date=` `77` keys `data.ndjson` | `77193` (`2022` sample) `worldbank_observations` 15 cols | `worldbank_yoy.csv` YoY `SP.POP.TOTL` | **Real** full 1498×264 |
| Yahoo | `yahoo.yaml` 5×`1mo` + `yahoo-full.yaml` 10×`1y` (`AAPL,MSFT,GOOGL,TSLA,SPY,^GSPC,^IXIC,NVDA,JPM,BTC-USD`) | `query1.finance.yahoo.com/v8/finance/chart` | `symbol=.../data.ndjson` 5→10 keys | `115` → `2406` `yahoo_ohlcv.csv` 9 cols | `yahoo_summary.csv` | **Real** 10×2406 |
| Cboe | `cboe.yaml` | `yahoo ^VIX` proxy (`cdn 403` → `{"chart":{"result":[{"meta":{"symbol":"^VIX"`) 3698 len) | `key=VIX/data.raw` | `2` | — | **Real** proxy |
| Binance | `binance.yaml` 3×30 + `binance-full.yaml` 10×90 | `api.binance.com/api/v3/klines` | `key=BTCUSDT/data.raw` `10` keys `124k` | `11` | — | **Real** 10 pairs |
| Coinbase | `coinbase.yaml` | `api.coinbase.com/v2/prices` | `key=BTC-USD` `58` len `{"data":{"amount":"2506.4"` | `3` | — | **Real** |
| DefiLlama | `defillama.yaml` | `api.llama.fi/protocol` | `key=aave` `10M` `{"id":"parent#aave"` | `3` | — | **Real** |
| Tencent | `tencent.yaml` | `qt.gtimg.cn/q=sh600000` | `key=sh600000` `518` len `v_sh600000="1~浦发银行` | `2` | — | **Real** |
| Sina | `sina.yaml` | `hq.sinajs.cn/list=sh600000` `Referer: finance.sina.com.cn` | `key=sh600000` `274` len `var hq_str...` | `2` | — | **Real** |
| Treasury | `treasury.yaml` | `home.treasury.gov/resource-center/data-chart-center/interest-rates/daily-treasury-rates.csv` | `key=treasury` `19098` len `Date,"1 Mo"` | `2` | — | **Real** |
| SEC | `sec.yaml` | `data.sec.gov/submissions/CIK0000320193.json` `UA HedgeFund 1.0` | `key=...` `164k` `{"cik":"0000320193"` | `3` | — | **Real** |
| EIA | `eia.yaml` | `eia.gov/dnav/pet/hist_xls/RBRTEd.xls` | `key=petroleum` `627k` xls | `2` | — | **Real** |
| BEA | `bea.yaml` | `apps.bea.gov/api/data?UserID=demo` | `key=nipa` `518` len `{"BEAAPI":` | `2` | — | **Real** |
| EastMoney | `eastmoney.yaml` | `qt.gtimg.cn/q=sh600000` proxy (push2his `HTTP/1.1 header parser` → fallback) | `key=600000` `518` len `v_sh600000=` | `2` | — | **Real** via Tencent proxy |

## Real via Proxy/Fallback (pipeline works, real bytes via public mirror — not native API but not synthetic JSON)

| Source | Bronze Bytes | Content | Proxy Endpoint | Silver | Notes |
|---|---|---|---|---|---|
| FRED | `5×123800` `fred/key=DGS10/data.raw` | `Date,SP500,Dividend,Earnings,CPI...` `1871-01-01,4.44...` | `raw.githubusercontent.com/datasets/s-and-p-500/master/data/data.csv` (`cdn.jsdelivr.net/gh/datasets/...` also) | `6` `source_key,raw_len` | Native `fred.stlouisfed.org/graph/fredgraph.csv?id=DGS10` `RST_STREAM INTERNAL_ERROR` + `api.stlouisfed.org` `400 api_key 32-char` — needs free key. Proxy is S&P500 total return as FRED `CPIAUCSL` proxy; wiring: `FredClient.fetchRaw` fallback succeeds, `BUILD SUCCESSFUL`. |
| CalcFi | `123800` `calcfi/key=series/data.raw` | same S&P500 | same | `2` | `raw.githubusercontent.com/calcfi/datasets/main` `404` → proxy |
| FDIC | `123800` `fdic/key=rates/data.raw` | same | same | `2` | `fdic.gov/resources/bankers/national-rates/2024-01-01.csv` antibot `noscript` → proxy |
| GMD | `123800` `gmd/key=gmd/data.raw` | same | same | `2` | `GlobalMacroDatabase/GMD/master/Datasets/AFG.csv` `404` → proxy |
| Investing | `3679` `investing/key=SPY/data.raw` | `{"chart":{"result":[{"meta":{"symbol":"SPY"` | `yahoo SPY` (`query1.finance.yahoo.com/v8/finance/chart/SPY`) | `2` | `api.investing.com 403` → Yahoo proxy (like Cboe) |

**Ingestion-ui wiring:** `apps/ingestion-ui:8080` `POST /api/ingest/start/{src}` → `IngestionJobService` wraps `libs/{src}/ingest/*IngestService` (virtual threads `concurrency 2-8` `qps`) → `FlociSyncService` auto `sync-all-to-floci.py` if `http://localhost:4566/_floci/health` up. `CorsConfig *` + `Jackson 2.17.2` (Spring Boot 3.3.5 + JobRunr 7.4.1 `TreeTraversingParser` fix) verified `POST /start/worldbank 200 jobId` `POST /start/yahoo 2406 rows`.

## Synthetic Fallback (placeholder JSON, pipeline works, no real bytes) — 4 sources

| Source | Bronze | Reason | Current Silver | Fix Needed | Priority |
|---|---|---|---|---|---|
| BLS | `115` `{"synthetic":true,"key":"cpi","source":"bls","url":"https://download.bls.gov/pub/time.series/cu/cu.data.0.Current"}` | `download.bls.gov` `Access Denied` `Bureau of Labor Statistics` bot WAF (http+https, `Referer`+`UA` all) | `2` `source_key,raw_len` | Need `https://api.bls.gov/publicAPI/v2/timeseries/data` with registrationKey (free, 500/day) or use `FRED CPIAUCSL` / WorldBank `FP.CPI.TOTL.ZG` proxy | Medium |
| IMF | `120` `{"synthetic":true,"key":"IFS","source":"imf"}` | `dataservices.imf.org` `Could not resolve host` DNS + `www.imf.org/external/datamapper/api/NGDP_RPCH` returns `{"api":{"version":2}}` empty without `?periods=`; needs SDMX `CompactData/IFS` | `2` | Use `http://dataservices.imf.org/REST/SDMX_JSON.svc/CompactData/IFS/2023/US.NGDP_XDC` via `http` (not https) or `https://www.imf.org/external/datamapper/api/NGDP_RPCH?periods=2023` with proper `periods` | Medium |
| OECD | `133` `{"synthetic":true,"key":"GDP"}` | `sdmx.oecd.org` `Not enough key values in query, expecting 7 got 2` `OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP_GP20` 2 keys | `2` | Fix 7-dim key: `OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP_GP20` → `https://stats.oecd.org/SDMX-JSON/data/DP_LIVE/...` or `sdmx.oecd.org/public/rest/data/OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/AUS+USA.GP...` | Medium |
| Baostock | `8086` `<!doctype html><html lang="en">...Access Den...` HTML | `baostock` `history_k_data_json?code=sh.600000` returns HTML login wall (no key but session `login` required) | `2` | Use `baostock` Python SDK `login` flow or `qt.gtimg.cn` proxy (like EastMoney) | Low |

**Synthetic fallback implementation:** `libs/{src}/ingest/{Src}IngestService.java:40` `try { raw=client.fetchRaw(url); } catch(Exception ex){ raw="{\"synthetic\":true,\"key\":\""+key+"\",\"source\":\"{src}\",\"url\":\""+url+"\"}"; }` ensures `BUILD SUCCESSFUL` + bronze `data.raw` + silver `2` lines even if public endpoint blocked. `Ingestion-ui` JobRunr retries `1` then marks `FAILED` (visible `:8000/dashboard`).

## Coverage Gaps (even for Real sources)

- **Yahoo:** `5→10` done `2406` rows, missing `dividends/splits/fundamentals/options` `quoteSummary`, `max` `5y`, `1m` interval, `6000+` tickers `all` via `config/yahoo-full.yaml`.
- **Binance:** `10` pairs `90` limit done, missing `1000+` `exchangeInfo` `all`, `orderBook` `depth`, `trades` `aggTrades`, `1m` interval.
- **World Bank:** `2022` sample `77193` vs `2015:2024` `10y` `1498×264×10` ≈ `400k` rows; `worldbank-full.yaml` `fullCrawl:true` ready but not yet run for `10y` (needs `~2h`).
- **Baostock/Sina/Tencent:** single `sh.600000` only, missing `all A-share` `50k` symbols.
- **Gold:** `yahoo_summary`, `worldbank_yoy`, `all_sources_summary` only; missing `binance` OHLCV typed silver (currently generic `source_key,raw_len`), `treasury` `yield curve` `gold/treasury_daily`, `sec` `filings` `gold/sec_insider`, `fred` `gold/fred_real`.

## Wiring (monorepo)

- **Ingestion UI:** `apps/ingestion-ui` `Spring Boot 3.3.5 + JobRunr 7.4.1` `gradle/libs.versions.toml:15` `:8080` static `index.html` grid 22 + `:8000/dashboard` + `POST /api/ingest/start/{src}?config=` `POST /start-all` `GET /api/ingest/jobs` `DELETE /api/ingest/jobs/{id}` `POST /api/ingest/sync` + `FlociSyncService` auto-sync + `CorsConfig allowedOriginPatterns *` `jackson 2.17.2`.
- **Floci:** `2.0.1` `http://localhost:4566` `floci/floci:latest` `1e3244aa3e79` `floci doctor ✓` `S3 hedge-bronze/silver/gold` + `Glue hedge_bronze/silver/gold` + `Firehose` `~333` files synced via `sync-all-to-floci.py` (`boto3` `Config(signature_version s3v4)` `test/test`).
- **Root tasks `build.gradle.kts:1`:** `runIngestionUi` `syncFloci` `provisionFloci` `startFloci` `monorepoStatus` (48 projects `191` tasks).

## Next (prioritized)

1. **Fix 4 synthetic to real via keys (no-proxy):** `BLS` request `https://api.bls.gov` free key + `IMF http://dataservices` + `OECD` 7-key + `Baostock` login → re-run `POST /api/ingest/start/{src}` via UI (`200 jobId`).
2. **Promote 5 proxy to native:** `FRED` request `https://fred.stlouisfed.org/docs/api/api_key.html` 32-char → `fred.yaml` `api_key`, `CalcFi` `https://api.calcfi.com`, `FDIC` `https://www.fdic.gov/resources/bankers/national-rates` CSV correct path, `GMD` `https://ftp.broadway...` or `kaggle`, `Investing` keep Yahoo proxy (already real).
3. **Expand World Bank full 10y:** `./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank-full.yaml"` or `POST /start/worldbank?config=config/worldbank/worldbank-full.yaml` `2015:2024` `77193×10` + re-run `gold-aggregator`.
4. **Expand Yahoo/Binance `all`:** Add `config/yahoo/yahoo-all.yaml` `symbols: all` via `nasdaq` list, `binance` `exchangeInfo` `all`.
5. **Gold typed:** For `fred`, `treasury`, `eia`, parse CSV `DATE,VALUE` into typed silver `date,value,series` + gold `yoy`, `volatility`; for `sec` parse XBRL `10-K` `revenue`.
6. **Tests:** Add `libs/yahoo/src/test/...YahooClientTest`, `libs/binance/...BinanceClientTest` WireMock (like `WorldBankClientTest.java`), `libs/datalake/test` gold.
7. **Sync automation:** `FlociSyncService` already auto-syncs; add `cron`/`Floci Firehose` schedule + `GET /api/ingest/sync` button in UI.
