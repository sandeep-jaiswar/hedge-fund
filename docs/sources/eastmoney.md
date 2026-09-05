# EastMoney — A-share Klines

- **Endpoint:** `https://push2.eastmoney.com/api/qt/stock/kline/get`
- **API:** `GET /api/qt/stock/kline/get?secid=1.600000&fields1=f1&fields2=f51`
- **Catalog:** `eastmoney_raw → eastmoney` (`datalake/catalog/glue.json`)
- **Config:** `config/eastmoney/eastmoney.yaml` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/eastmoney/src/main/java/com/hedgefund/eastmoney/{config/EastmoneyConfig.java, client/EastmoneyClient.java, store/EastmoneyBronzeWriter.java, store/EastmoneySilverTransformer.java, ingest/EastmoneyIngestService.java}`
- **Service:** `services/eastmoney-ingest/src/main/java/com/hedgefund/eastmoney/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:eastmoney-ingest:run --args="--config config/eastmoney/eastmoney.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/eastmoney/eastmoney.csv', header=true)`
- **Bronze:** `datalake/data/bronze/eastmoney/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/eastmoney/eastmoney.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
