# BLS — CPI/Employment

- **Endpoint:** `https://download.bls.gov/pub/time.series/cu`
- **API:** `Bulk TXT cu.data.0.Current (raw.github proxy)`
- **Catalog:** `bls_raw → bls` (`datalake/catalog/glue.json`)
- **Config:** `config/bls/bls.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/bls/src/main/java/com/hedgefund/bls/{config/BlsConfig.java, client/BlsClient.java, store/BlsBronzeWriter.java, store/BlsSilverTransformer.java, ingest/BlsIngestService.java}`
- **Service:** `services/bls-ingest/src/main/java/com/hedgefund/bls/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:bls-ingest:run --args="--config config/bls/bls.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/bls/bls.csv', header=true)`
- **Bronze:** `datalake/data/bronze/bls/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/bls/bls.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
