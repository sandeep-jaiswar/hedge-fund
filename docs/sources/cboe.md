# Cboe — VIX

- **Endpoint:** `https://query1.finance.yahoo.com/v8/finance/chart/%5EVIX`
- **API:** `Yahoo proxy for cdn.cboe.com 403, same as VIX`
- **Catalog:** `cboe_raw → cboe (source_key,raw_len)` (`datalake/catalog/glue.json`)
- **Config:** `config/cboe/cboe.yaml symbols:[VIX]` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/cboe/src/main/java/com/hedgefund/cboe/{config/CboeConfig.java, client/CboeClient.java, store/CboeBronzeWriter.java, store/CboeSilverTransformer.java, ingest/CboeIngestService.java}`
- **Service:** `services/cboe-ingest/src/main/java/com/hedgefund/cboe/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:cboe-ingest:run --args="--config config/cboe/cboe.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/cboe/cboe.csv', header=true)`
- **Bronze:** `datalake/data/bronze/cboe/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/cboe/cboe.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
