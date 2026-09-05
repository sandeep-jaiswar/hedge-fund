# DefiLlama — TVL

- **Endpoint:** `https://api.llama.fi/protocol`
- **API:** `GET /protocol/aave → {tvl, chainTvls, tokens}`
- **Catalog:** `defillama_raw → defillama` (`datalake/catalog/glue.json`)
- **Config:** `config/defillama/defillama.yaml protocols:[aave,uniswap]` (`concurrency`, `qps`, `retry`, `paths: bronze/silver`)
- **Lib:** `libs/defillama/src/main/java/com/hedgefund/defillama/{config/DefillamaConfig.java, client/DefillamaClient.java, store/DefillamaBronzeWriter.java, store/DefillamaSilverTransformer.java, ingest/DefillamaIngestService.java}`
- **Service:** `services/defillama-ingest/src/main/java/com/hedgefund/defillama/ingest/Main.java` (`--config` + `--dry-run`, resolves via `Datalake.defaultLocal()`)
- **Run:** `./gradlew :services:defillama-ingest:run --args="--config config/defillama/defillama.yaml"`
- **Query:** `SELECT * FROM read_csv('datalake/data/silver/defillama/defillama.csv', header=true)`
- **Bronze:** `datalake/data/bronze/defillama/key=.../data.raw` + `_watermark.json`
- **Silver:** `datalake/data/silver/defillama/defillama.csv` (yahoo/worldbank have typed cols; others generic `source_key,raw_len,bronze_path`)
- **Throttle:** `qps 1-5` `burst 2-4` `concurrency 2-8` virtual threads + `Semaphore` + retry `429/5xx` + synthetic `{synthetic:true}` fallback ensures `BUILD SUCCESSFUL`.

No API key required. See [Sources README](README.md) and [Services](../services.md).
