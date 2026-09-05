# OECD — Economic

- **Endpoint:** `https://sdmx.oecd.org/public/rest/data`
- **API:** `GET /public/rest/data/OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP_GP20`
- **Catalog:** `oecd_raw → oecd` (`datalake/catalog/glue.json`)
- **Config:** `config/oecd/oecd.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/oecd/src/main/java/com/hedgefund/oecd/{config/OecdConfig.java, client/OecdClient.java, store/OecdBronzeWriter.java, store/OecdSilverTransformer.java, ingest/OecdIngestService.java}`
- **Service:** `services/oecd-ingest/src/main/java/com/hedgefund/oecd/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:oecd-ingest:run --args="--config config/oecd/oecd.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/oecd/oecd.csv', header=true)`
- **Bronze:** `datalake/data/bronze/oecd/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/oecd/oecd.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
