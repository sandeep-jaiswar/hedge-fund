# IMF — IFS/BOP

- **Endpoint:** `https://dataservices.imf.org/REST/SDMX_JSON.svc/CompactData/IFS`
- **API:** `SDMX JSON CompactData/IFS/2023/US.NGDP_XDC (llama proxy fallback)`
- **Catalog:** `imf_raw → imf` (`datalake/catalog/glue.json`)
- **Config:** `config/imf/imf.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/imf/src/main/java/com/hedgefund/imf/{config/ImfConfig.java, client/ImfClient.java, store/ImfBronzeWriter.java, store/ImfSilverTransformer.java, ingest/ImfIngestService.java}`
- **Service:** `services/imf-ingest/src/main/java/com/hedgefund/imf/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:imf-ingest:run --args="--config config/imf/imf.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/imf/imf.csv', header=true)`
- **Bronze:** `datalake/data/bronze/imf/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/imf/imf.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
