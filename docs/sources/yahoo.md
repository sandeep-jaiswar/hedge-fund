# Yahoo Finance — OHLCV

- **Endpoint:** `https://query1.finance.yahoo.com/v8/finance/chart`
- **API:** `GET /v8/finance/chart/AAPL?interval=1d&range=1mo → {chart:{result:[{meta:{symbol,currency},timestamp:[...],indicators:{quote:[{open,high,low,close,volume}],adjclose:[{adjclose}]}]}}`
- **Catalog:** `yahoo_raw (7 cols) → yahoo_ohlcv (9 cols: symbol,date,epoch,open,high,low,close,adj_close,volume)` (`datalake/catalog/glue.json`)
- **Config:** `config/yahoo/yahoo.yaml symbols:[AAPL,MSFT,GOOGL,TSLA,SPY] interval:1d range:1mo` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/yahoo/src/main/java/com/hedgefund/yahoo/{config/YahooConfig.java, client/YahooClient.java, store/YahooBronzeWriter.java, store/YahooSilverTransformer.java, ingest/YahooIngestService.java}`
- **Service:** `services/yahoo-ingest/src/main/java/com/hedgefund/yahoo/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:yahoo-ingest:run --args="--config config/yahoo/yahoo.yaml"`
- **Query:** `SELECT symbol, avg(close) FROM read_csv('datalake/data/silver/yahoo/yahoo_ohlcv.csv', header=true) GROUP BY symbol`
- **Bronze:** `datalake/data/bronze/yahoo/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/yahoo/yahoo.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
