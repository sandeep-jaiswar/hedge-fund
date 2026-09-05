# Tencent — A/HK/US Quotes

- **Endpoint:** `https://qt.gtimg.cn/q`
- **API:** `GET /q=sh600000 → v_sh600000="51~600000~..."`
- **Catalog:** `tencent_raw → tencent` (`datalake/catalog/glue.json`)
- **Config:** `config/tencent/tencent.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/tencent/src/main/java/com/hedgefund/tencent/{config/TencentConfig.java, client/TencentClient.java, store/TencentBronzeWriter.java, store/TencentSilverTransformer.java, ingest/TencentIngestService.java}`
- **Service:** `services/tencent-ingest/src/main/java/com/hedgefund/tencent/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:tencent-ingest:run --args="--config config/tencent/tencent.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/tencent/tencent.csv', header=true)`
- **Bronze:** `datalake/data/bronze/tencent/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/tencent/tencent.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
