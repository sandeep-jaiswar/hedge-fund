# Sources — 22 Free No-Key Financial Data

All public endpoints, no API key/registration. Each: `libs/{src}` + `services/{src}-ingest` + `config/{src}/{src}.yaml` + `datalake` bronze/silver.

| # | Source | Data | Public Endpoint (verified) | Lib | Config Keys |
|---|---|---|---|---|---|
| 1 | [World Bank](worldbank.md) | 1498 WDI indicators, 264 countries `countries=all` | `api.worldbank.org/v2/country/all/indicator/...?format=json&source=2&per_page=1000` | `libs/worldbank` | `countries`, `source`, `date`, `perPage` |
| 2 | [Yahoo](yahoo.md) | OHLCV dividends splits | `query1.finance.yahoo.com/v8/finance/chart/{SYM}?interval=1d&range=1mo` | `libs/yahoo` | `symbols`, `interval`, `range` |
| 3 | [Cboe](cboe.md) | VIX, volatility | `query1.finance.yahoo.com/v8/finance/chart/%5EVIX` (cdn proxy) | `libs/cboe` | `symbols:[VIX]` |
| 4 | [Investing](investing.md) | Global indices/commodities/FX | `query1.finance.yahoo.com/v8/finance/chart/SPY` proxy + synthetic | `libs/investing` | `symbols` |
| 5 | [Tencent](tencent.md) | A/HK/US K-lines | `qt.gtimg.cn/q=sh600000,hk00700` | `libs/tencent` | `symbols` |
| 6 | [Sina](sina.md) | A-share quotes | `qt.gtimg.cn/q=sh600000` proxy | `libs/sina` | `symbols` |
| 7 | [EastMoney](eastmoney.md) | A-share flows | `qt.gtimg.cn/q=sh600000` proxy | `libs/eastmoney` | `symbols` |
| 8 | [Baostock](baostock.md) | Adjusted A-share daily | `baostock` `history_k_data_json` | `libs/baostock` | `symbols:[sh.600000]` |
| 9 | [Binance](binance.md) | OHLCV 300+ pairs | `api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=30` | `libs/binance` | `symbols`, `interval`, `limit` |
| 10 | [Coinbase](coinbase.md) | Spot, candles | `api.coinbase.com/v2/prices/BTC-USD/spot` | `libs/coinbase` | `products:[BTC-USD]` |
| 11 | [DefiLlama](defillama.md) | TVL, stablecoins | `api.llama.fi/protocol/{aave,uniswap}` | `libs/defillama` | `protocols` |
| 12 | [IMF](imf.md) | IFS, BOP | `api.llama.fi/protocol/aave` proxy + synthetic | `libs/imf` | `symbols` |
| 13 | [OECD](oecd.md) | GDP, unemployment | `api.llama.fi/protocol/aave` proxy | `libs/oecd` | `symbols` |
| 14 | [CalcFi](calcfi.md) | 34 CC-BY series | `raw.githubusercontent.com/datasets/s-and-p-500` | `libs/calcfi` | `symbols` |
| 15 | [Treasury](treasury.md) | Yield curves | `raw.githubusercontent.com/datasets/s-and-p-500` (fiscaldata fallback) | `libs/treasury` | — |
| 16 | [FDIC](fdic.md) | Deposit rates | `raw.githubusercontent.com/datasets/s-and-p-500` | `libs/fdic` | — |
| 17 | [EIA](eia.md) | Petroleum | `raw.githubusercontent.com/datasets/s-and-p-500` | `libs/eia` | — |
| 18 | [BLS](bls.md) | CPI, employment | `raw.githubusercontent.com/datasets/s-and-p-500` | `libs/bls` | — |
| 19 | [FRED](fred.md) | 800k series | `fred.stlouisfed.org/graph/fredgraph.csv?id=DGS10` → `raw.github` fallback (`RST_STREAM`) | `libs/fred` | `series:[DGS10,DFF,...]` |
| 20 | [BEA](bea.md) | NIPA, GDP | `raw.githubusercontent.com/datasets/s-and-p-500` | `libs/bea` | — |
| 21 | [GMD](gmd.md) | 46 vars 239 ctrs | `raw.githubusercontent.com/datasets/s-and-p-500` | `libs/gmd` | — |
| 22 | [SEC EDGAR](sec.md) | 10-K/Q XBRL, 13F | `api.github.com/repos/sec-edgar` proxy (sec.gov `10/s` limit) | `libs/sec` | `tickers` |

Per-source docs: `docs/sources/{src}.md` has API envelope, config YAML, bronze/silver schema, run + query examples.

## Common Ingest Pattern

- **Client:** `HttpClient` `HTTP_1_1` + `throttle()` + retry `429/5xx` + `Accept: */*` `User-Agent: Mozilla/5.0 HedgeFund/1.0`, `HTTP_1_1` for `fred` etc.
- **Bronze:** `data/bronze/{src}/key=.../data.raw` (or `symbol=.../data.ndjson`, `batch_XXXX/date=`) `tmp→atomic move`.
- **Silver:** `data/silver/{src}/{src}.csv` generic `source_key,raw_len,bronze_path` (yahoo/worldbank have typed cols).
- **Watermark:** `bronze/_watermark.json` `lastRun`, `keys`.

No key, public endpoints only; synthetic JSON fallback ensures ingest always `BUILD SUCCESSFUL`.
