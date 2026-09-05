# Investing.com — Global Indices

- **Endpoint:** `https://query1.finance.yahoo.com/v8/finance/chart/SPY (proxy)`
- **API:** `Investing proxy via Yahoo SPY`
- **Catalog:** `investing_raw → investing` (`datalake/catalog/glue.json`)
- **Config:** `config/investing/investing.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/investing/src/main/java/com/hedgefund/investing/{config/InvestingConfig.java, client/InvestingClient.java, store/InvestingBronzeWriter.java, store/InvestingSilverTransformer.java, ingest/InvestingIngestService.java}`
- **Service:** `services/investing-ingest/src/main/java/com/hedgefund/investing/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:investing-ingest:run --args="--config config/investing/investing.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/investing/investing.csv', header=true)`
- **Bronze:** `datalake/data/bronze/investing/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/investing/investing.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
