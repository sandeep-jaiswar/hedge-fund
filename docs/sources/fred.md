# FRED — 800k Series (Bulk CSV, no key)

- **Endpoint:** `https://fred.stlouisfed.org/graph/fredgraph.csv`
- **API:** `GET /graph/fredgraph.csv?id=DGS10 → DATE,VALUE CSV (HTTP_1_1, fallback raw.github)`
- **Catalog:** `fred_raw → fred (source_key,raw_len)` (`datalake/catalog/glue.json`)
- **Config:** `config/fred/fred.yaml series:[DGS10,DFF,UNRATE,CPIAUCSL,T10Y2Y]` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/fred/src/main/java/com/hedgefund/fred/{config/FredConfig.java, client/FredClient.java, store/FredBronzeWriter.java, store/FredSilverTransformer.java, ingest/FredIngestService.java}`
- **Service:** `services/fred-ingest/src/main/java/com/hedgefund/fred/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:fred-ingest:run --args="--config config/fred/fred.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/fred/fred.csv', header=true)`
- **Bronze:** `datalake/data/bronze/fred/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/fred/fred.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
