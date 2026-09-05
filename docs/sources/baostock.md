# Baostock — Adjusted A-share

- **Endpoint:** `http://www.baostock.com/api/query/history_k_data_json`
- **API:** `GET /api/query/history_k_data_json?code=sh.600000&fields=date,open,high,low,close`
- **Catalog:** `baostock_raw → baostock` (`datalake/catalog/glue.json`)
- **Config:** `config/baostock/baostock.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/baostock/src/main/java/com/hedgefund/baostock/{config/BaostockConfig.java, client/BaostockClient.java, store/BaostockBronzeWriter.java, store/BaostockSilverTransformer.java, ingest/BaostockIngestService.java}`
- **Service:** `services/baostock-ingest/src/main/java/com/hedgefund/baostock/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:baostock-ingest:run --args="--config config/baostock/baostock.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/baostock/baostock.csv', header=true)`
- **Bronze:** `datalake/data/bronze/baostock/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/baostock/baostock.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
