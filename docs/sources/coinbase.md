# Coinbase — Spot

- **Endpoint:** `https://api.coinbase.com/v2/prices`
- **API:** `GET /v2/prices/BTC-USD/spot → {data:{amount,base,currency}}`
- **Catalog:** `coinbase_raw → coinbase` (`datalake/catalog/glue.json`)
- **Config:** `config/coinbase/coinbase.yaml products:[BTC-USD,ETH-USD]` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/coinbase/src/main/java/com/hedgefund/coinbase/{config/CoinbaseConfig.java, client/CoinbaseClient.java, store/CoinbaseBronzeWriter.java, store/CoinbaseSilverTransformer.java, ingest/CoinbaseIngestService.java}`
- **Service:** `services/coinbase-ingest/src/main/java/com/hedgefund/coinbase/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:coinbase-ingest:run --args="--config config/coinbase/coinbase.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/coinbase/coinbase.csv', header=true)`
- **Bronze:** `datalake/data/bronze/coinbase/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/coinbase/coinbase.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
