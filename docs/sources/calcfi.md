# CalcFi — 34 CC-BY Series

- **Endpoint:** `https://raw.githubusercontent.com/datasets/s-and-p-500`
- **API:** `Direct CSV/JSON URLs (raw.github proxy)`
- **Catalog:** `calcfi_raw → calcfi` (`datalake/catalog/glue.json`)
- **Config:** `config/calcfi/calcfi.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/calcfi/src/main/java/com/hedgefund/calcfi/{config/CalcfiConfig.java, client/CalcfiClient.java, store/CalcfiBronzeWriter.java, store/CalcfiSilverTransformer.java, ingest/CalcfiIngestService.java}`
- **Service:** `services/calcfi-ingest/src/main/java/com/hedgefund/calcfi/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:calcfi-ingest:run --args="--config config/calcfi/calcfi.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/calcfi/calcfi.csv', header=true)`
- **Bronze:** `datalake/data/bronze/calcfi/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/calcfi/calcfi.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
