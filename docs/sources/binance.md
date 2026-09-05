# Binance — Crypto OHLCV

- **Endpoint:** `https://api.binance.com/api/v3/klines`
- **API:** `GET /api/v3/klines?symbol=BTCUSDT&interval=1d&limit=30 → [[openTime,open,high,low,close,volume,closeTime,quoteVol,trades]]`
- **Catalog:** `binance_raw → binance (source_key,raw_len)` (`datalake/catalog/glue.json`)
- **Config:** `config/binance/binance.yaml symbols:[BTCUSDT,ETHUSDT,BNBUSDT] interval:1d limit:30` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/binance/src/main/java/com/hedgefund/binance/{config/BinanceConfig.java, client/BinanceClient.java, store/BinanceBronzeWriter.java, store/BinanceSilverTransformer.java, ingest/BinanceIngestService.java}`
- **Service:** `services/binance-ingest/src/main/java/com/hedgefund/binance/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:binance-ingest:run --args="--config config/binance/binance.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/binance/binance.csv', header=true)`
- **Bronze:** `datalake/data/bronze/binance/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/binance/binance.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
