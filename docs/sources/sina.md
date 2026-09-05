# Sina — A-share Quotes

- **Endpoint:** `https://hq.sinajs.cn/list`
- **API:** `GET /list=sh600000 → var hq_str_sh600000="..."`
- **Catalog:** `sina_raw → sina` (`datalake/catalog/glue.json`)
- **Config:** `config/sina/sina.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/sina/src/main/java/com/hedgefund/sina/{config/SinaConfig.java, client/SinaClient.java, store/SinaBronzeWriter.java, store/SinaSilverTransformer.java, ingest/SinaIngestService.java}`
- **Service:** `services/sina-ingest/src/main/java/com/hedgefund/sina/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:sina-ingest:run --args="--config config/sina/sina.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/sina/sina.csv', header=true)`
- **Bronze:** `datalake/data/bronze/sina/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/sina/sina.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
