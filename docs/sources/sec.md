# SEC EDGAR — Filings

- **Endpoint:** `https://www.sec.gov/Archives + data.sec.gov/submissions/CIK*.json`
- **API:** `GET /Archives/edgar/data/{CIK}/... + /submissions/CIK0000320193.json (10/s, UA required, proxy fallback)`
- **Catalog:** `sec_raw → sec` (`datalake/catalog/glue.json`)
- **Config:** `config/sec/sec.yaml tickers:[AAPL,MSFT]` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/sec/src/main/java/com/hedgefund/sec/{config/SecConfig.java, client/SecClient.java, store/SecBronzeWriter.java, store/SecSilverTransformer.java, ingest/SecIngestService.java}`
- **Service:** `services/sec-ingest/src/main/java/com/hedgefund/sec/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:sec-ingest:run --args="--config config/sec/sec.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/sec/sec.csv', header=true)`
- **Bronze:** `datalake/data/bronze/sec/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/sec/sec.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
