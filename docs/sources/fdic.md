# FDIC — Deposit Rates

- **Endpoint:** `https://www.fdic.gov/resources/bankers/national-rates`
- **API:** `Bulk CSV (raw.github proxy)`
- **Catalog:** `fdic_raw → fdic` (`datalake/catalog/glue.json`)
- **Config:** `config/fdic/fdic.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/fdic/src/main/java/com/hedgefund/fdic/{config/FdicConfig.java, client/FdicClient.java, store/FdicBronzeWriter.java, store/FdicSilverTransformer.java, ingest/FdicIngestService.java}`
- **Service:** `services/fdic-ingest/src/main/java/com/hedgefund/fdic/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:fdic-ingest:run --args="--config config/fdic/fdic.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/fdic/fdic.csv', header=true)`
- **Bronze:** `datalake/data/bronze/fdic/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/fdic/fdic.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
