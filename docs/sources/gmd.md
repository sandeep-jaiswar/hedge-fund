# GMD — Global Macro Database

- **Endpoint:** `https://raw.githubusercontent.com/GlobalMacroDatabase/GMD`
- **API:** `GMD.csv bulk 46 vars 239 ctrs`
- **Catalog:** `gmd_raw → gmd` (`datalake/catalog/glue.json`)
- **Config:** `config/gmd/gmd.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/gmd/src/main/java/com/hedgefund/gmd/{config/GmdConfig.java, client/GmdClient.java, store/GmdBronzeWriter.java, store/GmdSilverTransformer.java, ingest/GmdIngestService.java}`
- **Service:** `services/gmd-ingest/src/main/java/com/hedgefund/gmd/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:gmd-ingest:run --args="--config config/gmd/gmd.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/gmd/gmd.csv', header=true)`
- **Bronze:** `datalake/data/bronze/gmd/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/gmd/gmd.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
