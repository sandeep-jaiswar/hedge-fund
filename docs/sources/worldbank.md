# World Bank — Global Development Indicators

- **Endpoint:** `https://api.worldbank.org/v2`
- **API:** `GET /v2/country/all/indicator/NY.GDP.MKTP.CD?format=json&source=2&per_page=1000&date=2015:2024 → [{page,pages,per_page,total},[{indicator:{id,value},country:{id,value},countryiso3code,date,value,unit,obs_status,decimal}]]`
- **Catalog:** `worldbank_raw (11 cols) → worldbank_observations (15 cols: indicator_id, country_iso3, date, value, year, is_valid...)` (`datalake/catalog/glue.json`)
- **Config:** `config/worldbank/worldbank.yaml countries:[all] source:2 date:2015:2024` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/worldbank/src/main/java/com/hedgefund/worldbank/{config/WorldbankConfig.java, client/WorldbankClient.java, store/WorldbankBronzeWriter.java, store/WorldbankSilverTransformer.java, ingest/WorldbankIngestService.java}`
- **Service:** `services/worldbank-ingest/src/main/java/com/hedgefund/worldbank/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:worldbank-ingest:run --args="--config config/worldbank/worldbank.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/worldbank/worldbank_observations/observations.csv', header=true) WHERE indicator_id='SP.POP.TOTL'`
- **Bronze:** `datalake/data/bronze/worldbank/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/worldbank/worldbank.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
