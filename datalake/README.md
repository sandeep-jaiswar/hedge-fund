# Hedge-Fund Datalake — Floci-compatible, Docker-free (local mode)

Local medallion architecture for hedge-fund, designed to be **Floci-compatible** so the same tables work when Floci Docker is re-enabled (`floci start` on :4566).

**Why no Docker now?** `floci` server is distributed as `floci/floci:latest` Docker image only (no standalone binary in 2.0.1 release). CLI (`~/.local/bin/floci`) is just the orchestrator. To keep local dev simple per request, this datalake runs **pure Java 21 + filesystem + DuckDB** (Athena equivalent) with no container. When you need Floci, run `floci start` and provision the same tables via `datalake/scripts/provision-floci.py` or `libs/datalake` Floci profile.

## Layout

```
datalake/
├── data/
│   ├── bronze/          # raw ingestion (NDJSON/CSV — Firehose-compatible)
│   │   ├── market_ticks/  -> s3://hedge-bronze/market_ticks/  (Floci S3 mapping)
│   │   └── orders/
│   ├── silver/          # cleaned / normalized
│   │   └── ohlcv/
│   └── gold/            # aggregated / business-ready
│       └── positions/
├── catalog/
│   └── glue.json        # local Glue Data Catalog mock (mirrors Floci Glue API)
└── scripts/
    ├── provision.py         # generate sample data (no AWS needed)
    └── provision-floci.py   # push same tables to Floci when Docker is up (Glue+S3+Firehose)
```

## Floci mapping (when Docker re-enabled)

| Local path | Floci S3 URI | Glue DB | Table | InputFormat |
|---|---|---|---|---|
| `data/bronze/market_ticks/` | `s3://hedge-bronze/market_ticks/` | `hedge_bronze` | `market_ticks` | CSV / JSON |
| `data/silver/ohlcv/` | `s3://hedge-silver/ohlcv/` | `hedge_silver` | `ohlcv` | Parquet (DuckDB) |
| `data/gold/positions/` | `s3://hedge-gold/positions/` | `hedge_gold` | `positions` | CSV |

Athena equivalent = DuckDB (`libs/datalake` QueryEngine) — same SQL as `SELECT sum(amount) FROM hedge_bronze.market_ticks`.

## Quick start (no Docker)

```bash
# 1. generate sample data (CSV/NDJSON)
python3 datalake/scripts/provision.py
# or via Gradle (runs Java provisioner)
./gradlew :libs:datalake:run --args="provision"

# 2. query via DuckDB (Athena mock)
./gradlew :libs:datalake:run --args="query \"SELECT symbol, avg(price) FROM read_csv('datalake/data/bronze/market_ticks/*.csv', header=true) GROUP BY symbol\""

# 3. run tests (no Docker)
./gradlew :libs:datalake:test
```

## Floci mode (when you want Docker)

```bash
floci start                         # start local AWS (needs Docker)
eval $(floci env)                   # export AWS_ENDPOINT_URL=http://localhost:4566
python3 datalake/scripts/provision-floci.py  # creates S3 buckets + Glue DBs/tables + Firehose

aws s3 ls s3://hedge-bronze/ --endpoint-url $AWS_ENDPOINT_URL
aws glue get-databases --endpoint-url $AWS_ENDPOINT_URL
aws athena start-query-execution --query-string "SELECT * FROM hedge_bronze.market_ticks LIMIT 5" --query-execution-context Database=hedge_bronze --endpoint-url $AWS_ENDPOINT_URL
```

## Java usage

```java
var lake = new Datalake(Path.of("datalake"));
lake.provisionSampleData(); // idempotent
try (var qe = new QueryEngine()) {
  var rows = qe.query("SELECT symbol, count(*) FROM read_csv('datalake/data/bronze/market_ticks/*.csv', header=true) GROUP BY symbol");
  rows.forEach(System.out::println);
}
// Floci mode (when container up):
// var flociLake = Datalake.floci(); // uses testcontainers-floci + AWS SDK v2
```

See `libs/datalake` for source.
