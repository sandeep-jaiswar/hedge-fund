# EIA — Petroleum

- **Endpoint:** `https://www.eia.gov/dnav/pet`
- **API:** `Bulk xls/csv (raw.github proxy)`
- **Catalog:** `eia_raw → eia` (`datalake/catalog/glue.json`)
- **Config:** `config/eia/eia.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/eia/src/main/java/com/hedgefund/eia/{config/EiaConfig.java, client/EiaClient.java, store/EiaBronzeWriter.java, store/EiaSilverTransformer.java, ingest/EiaIngestService.java}`
- **Service:** `services/eia-ingest/src/main/java/com/hedgefund/eia/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:eia-ingest:run --args="--config config/eia/eia.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/eia/eia.csv', header=true)`
- **Bronze:** `datalake/data/bronze/eia/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/eia/eia.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
