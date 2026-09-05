# US Treasury — Yield Curves (Bulk)

- **Endpoint:** `https://api.fiscaldata.treasury.gov / raw.github fallback`
- **API:** `Bulk CSV via fiscaldata page[size]=5 or raw.github s-and-p-500 proxy (synthetic fallback)`
- **Catalog:** `treasury_raw → treasury` (`datalake/catalog/glue.json`)
- **Config:** `config/treasury/treasury.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/treasury/src/main/java/com/hedgefund/treasury/{config/TreasuryConfig.java, client/TreasuryClient.java, store/TreasuryBronzeWriter.java, store/TreasurySilverTransformer.java, ingest/TreasuryIngestService.java}`
- **Service:** `services/treasury-ingest/src/main/java/com/hedgefund/treasury/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:treasury-ingest:run --args="--config config/treasury/treasury.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/treasury/treasury.csv', header=true)`
- **Bronze:** `datalake/data/bronze/treasury/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/treasury/treasury.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
