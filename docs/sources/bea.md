# BEA — NIPA/GDP

- **Endpoint:** `https://apps.bea.gov/api/data`
- **API:** `Bulk NIPA T10101 (raw.github proxy)`
- **Catalog:** `bea_raw → bea` (`datalake/catalog/glue.json`)
- **Config:** `config/bea/bea.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/bea/src/main/java/com/hedgefund/bea/{config/BeaConfig.java, client/BeaClient.java, store/BeaBronzeWriter.java, store/BeaSilverTransformer.java, ingest/BeaIngestService.java}`
- **Service:** `services/bea-ingest/src/main/java/com/hedgefund/bea/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:bea-ingest:run --args="--config config/bea/bea.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/bea/bea.csv', header=true)`
- **Bronze:** `datalake/data/bronze/bea/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/bea/bea.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
