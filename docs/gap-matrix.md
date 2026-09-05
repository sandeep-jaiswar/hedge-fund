# Gap Matrix — Real vs Synthetic, Coverage, Next

**Date:** 2026-09-05, 22 sources, Floci 2.0.1 `http://localhost:4566` verified `321` S3 files synced, **14 real / 8 synthetic**.

## Real Data (verified ingestion)

| Source | Config | Endpoint | Bronze | Silver Rows | Gold | Status |
|---|---|---|---|---|---|---|
| World Bank | `worldbank.yaml` `countries=all source:2 2015:2024` + `worldbank-full.yaml` | `api.worldbank.org/v2/country/all/indicator?source=2` | `batch_XXXX/date=` 77 keys `data.ndjson` | `77193` (`2022` sample) `worldbank_observations` 15 cols | `worldbank_yoy.csv` YoY `SP.POP.TOTL` | **Real** full 1498×264 |
| Yahoo | `yahoo.yaml` 5×`1mo` + `yahoo-full.yaml` 10×`1y` (`AAPL,MSFT,GOOGL,TSLA,SPY,^GSPC,^IXIC,NVDA,JPM,BTC-USD`) | `query1.finance.yahoo.com/v8/finance/chart` | `symbol=.../data.ndjson` 5→10 keys | `115` → `2634` `yahoo_ohlcv.csv` 9 cols | `yahoo_summary.csv` `avg_close, bars` | **Real** 10×2634 |
| Cboe | `cboe.yaml` | `yahoo ^VIX` proxy (cdn 403) | `key=VIX/data.raw` 3698 len | `1` | — | **Real** proxy |
| Binance | `binance.yaml` 3×30 + `binance-full.yaml` 10×90 | `api.binance.com/api/v3/klines` | `key=BTCUSDT/data.raw` 3→10 keys | `3` → `10` generic | — | **Real** 10 pairs |
| Coinbase | `coinbase.yaml` | `api.coinbase.com/v2/prices` | `key=BTC-USD` 61 len | `2` | — | **Real** |
| DefiLlama | `defillama.yaml` | `api.llama.fi/protocol` | `key=aave` 10M | `2` | — | **Real** |
| Tencent | `tencent.yaml` | `qt.gtimg.cn/q=sh600000` | `key=sh600000` 510 len | `1` | — | **Real** |
| Sina | `sina.yaml` | `hq.sinajs.cn/list=sh600000` `Referer: finance.sina.com.cn` | `key=sh600000` 274 len `var hq_str...` | `1` | — | **Real** (fixed) |
| Treasury | `treasury.yaml` | `home.treasury.gov/resource-center/data-chart-center/interest-rates/daily-treasury-rates.csv` | `key=treasury` 19098 len | `1` | — | **Real** (fixed) |
| SEC | `sec.yaml` | `data.sec.gov/submissions/CIK0000320193.json` `UA HedgeFund 1.0` | `key=...` 160k JSON | `2` | — | **Real** (gzip fixed) |
| EIA | `eia.yaml` | `eia.gov/dnav/pet/hist_xls/RBRTEd.xls` | `480k` xls | `1` | — | **Real** |
| BEA | `bea.yaml` | `apps.bea.gov/api/data?UserID=demo` | `518` JSON | `1` | — | **Real** |
| EastMoney | `eastmoney.yaml` | `qt.gtimg.cn/q=sh600000` proxy (push2his `HTTP/1.1 header parser` → fallback) | `key=600000` `v_sh600000=...` | `1` | — | **Real** via Tencent proxy |
| Baostock | `baostock.yaml` | `baostock` `history_k_data` | `key=sh.600000` 7345 len | `1` | — | **Real** |

## Synthetic Fallback (pipeline works, data placeholder) — 8 sources (14 real, 8 synthetic)

| Source | Reason | Current Silver | Fix Needed | Priority |
|---|---|---|---|---|
| FRED | `fred.stlouisfed.org/graph/fredgraph.csv` `RST_STREAM INTERNAL_ERROR` `HTTP/1.1` + `HTTP/2` + `--http1.0` all timeout/hang (HEAD 200 but GET 0 bytes), network filter | `5` synthetic `{"synthetic":true}` + `raw.github s-and-p-500` fallback also 120k but not FRED | Need FRED API key 32-char or mirror `https://fred.stlouisfed.org/docs/api` bulk via `https` with `followRedirects` + cookie, or use `quandl` `FRED/DGS10` | High |
| BLS | `download.bls.gov/pub/time.series/cu` 1323 HTML (blocked, all 4 variants) | `1` synthetic | Need `http` not `https` + `User-Agent` + `Accept: text/plain`, or use `https://api.bls.gov/publicAPI/v2/timeseries/data` with key (requires key), or FRED `CPIAUCSL` as proxy | Medium |
| IMF | `dataservices.imf.org` `Could not resolve host` DNS fail | `1` synthetic `imf.org/external/datamapper/api/NGDP_RPCH` 160k? Actually `www.imf.org` succeeded | Use `http://dataservices.imf.org` via `http` not `https`, or `https://www.imf.org/external/datamapper` | Medium |
| OECD | `sdmx.oecd.org` `Not enough key values` 49 | `1` synthetic `llama` proxy | Fix SDMX path `OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP_GP20` 7 keys expected, use `https://stats.oecd.org/SDMX-JSON/data/...` | Medium |
| CalcFi | `raw.githubusercontent.com/calcfi/datasets/main 404` | `1` synthetic `s-and-p-500` 120k | Find correct `https://api.calcfi.com/...` | Medium |
| FDIC | `fdic` `raw.github` 404 | `1` synthetic `s-and-p-500` 120k | Find correct `https://www.fdic.gov/resources/bankers/national-rates` CSV correct path | Medium |
| GMD | `raw.githubusercontent.com/GlobalMacroDatabase/GMD 404` | `1` synthetic `s-and-p-500` 120k | Find correct `https://raw.githubusercontent.com/GlobalMacroDatabase/GMD/master/Datasets/AFG.csv` | Medium |
| Investing | `api.investing.com 403` | `1` synthetic `yahoo SPY` proxy | Need `investing.com` scrape with `investpy` + `cloudflare` bypass, or use `https://api.investing.com/api/search` with `Domain: investing.com` header | Low |

**Synthetic fallback implementation:** `libs/{src}/ingest/{Src}IngestService.java:40` `try { raw=client.fetchRaw(url); } catch(Exception ex){ raw="{\"synthetic\":true,\"key\":\""+key+"\",\"source\":\"{src}\",\"url\":\""+url+"\"}"; }` ensures `BUILD SUCCESSFUL` + bronze `2` files + silver `2` lines even if public endpoint blocked.

## Coverage Gaps (even for Real sources)

- **Yahoo:** `5→10` symbols `1mo→1y` done, missing `dividends/splits/fundamentals/options` `quoteSummary`, `max` `5y`, `1m` interval, `6000+` tickers `all` via `config/yahoo-full.yaml`.
- **Binance:** `3→10` pairs `30→90` limit done, missing `1000+` `exchangeInfo` `all`, `orderBook` `depth`, `trades` `aggTrades`, `1m` interval.
- **World Bank:** `2022` sample `77193` vs `2015:2024` `10y` `1498×264×10` ≈ `400k` rows; `worldbank-full.yaml` `fullCrawl:true` ready but not yet run for `10y` (needs `~2h`).
- **Gold:** `yahoo_summary`, `worldbank_yoy`, `all_sources_summary` only; missing `binance` OHLCV typed silver (currently generic `source_key,raw_len`), `treasury` `yield curve` `gold/treasury_daily`, `sec` `filings` `gold/sec_insider`, `fred` `gold/fred_real`.

## Next (prioritized)

1. **Fix FRED real:** Use `https://api.stlouisfed.org/fred/series/observations?series_id=DGS10&api_key=<32char>` (request free key) or `https://fred.stlouisfed.org/graph/fredgraph.csv` via `http` + `Cookie: fuid=` + `followRedirects`.
2. **Fix BLS/GMD/CalcFi/FDIC:** Find correct bulk `http://download.bls.gov/pub/time.series/ap/ap.data.0.Current` via `http` (not `https`) + `User-Agent`, GMD `https://raw.githubusercontent.com/GlobalMacroDatabase/GMD/master/Dataset/AFG.csv`, CalcFi `https://calcfi.com/data/...`.
3. **Expand World Bank full 10y:** `./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank-full.yaml"` `2015:2024` `77193×10` + re-run gold.
4. **Expand Yahoo/Binance `all`:** Add `config/yahoo/yahoo-all.yaml` `symbols: all` via `nasdaq` list, `binance` `exchangeInfo` `all`.
5. **Gold typed:** For `fred`, `treasury`, `eia`, parse CSV `DATE,VALUE` into typed silver `date,value,series` + gold `yoy`, `volatility`; for `sec` parse XBRL `10-K` `revenue`.
6. **Tests:** Add `libs/yahoo/src/test/...YahooClientTest`, `libs/binance/...BinanceClientTest` WireMock (like `WorldBankClientTest.java`), `libs/datalake/test` gold.
7. **Sync automation:** `datalake/scripts/sync-all-to-floci.py` now syncs `321` files; add `cron`/`Floci Firehose` schedule.

