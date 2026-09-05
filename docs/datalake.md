# Datalake — Medallion, Floci-compatible

Local filesystem datalake that mirrors Floci (LocalStack S3 + Glue + Athena + Firehose) without Docker, and syncs when `floci start` (`2.0.1`, `http://localhost:4566`, `floci/floci:latest`).

## Layout

```
datalake/
├── data/
│   ├── bronze/                # raw (NDJSON/CSV, Firehose-compatible) — 24 dirs
│   │   ├── market_ticks/       -> s3://hedge-bronze/market_ticks/ (200 sample ticks)
│   │   ├── orders/            -> s3://hedge-bronze/orders/
│   │   ├── worldbank/         -> s3://hedge-bronze/worldbank/ (batch_XXXX/date=)
│   │   ├── yahoo/             -> s3://hedge-bronze/yahoo/ (symbol=AAPL/data.ndjson)
│   │   ├── binance/           -> s3://hedge-bronze/binance/ (key=BTCUSDT/data.raw)
│   │   ├── cboe/ ... gmd/     -> s3://hedge-bronze/{src}/
│   │   └── _watermark.json    # per-source lastRun
│   ├── silver/                # cleaned — 23 dirs
│   │   ├── ohlcv/             # sample 26 rows
│   │   ├── worldbank/worldbank_observations/observations.csv (15 cols, 77193 rows for 2022)
│   │   ├── yahoo/yahoo_ohlcv.csv (symbol,date,epoch,open,high,low,close,adj_close,volume) 115 rows
│   │   ├── binance/binance.csv, coinbase/coinbase.csv, ... gmd/gmd.csv (2-6 lines each generic)
│   │   └── gold not yet
│   └── gold/                  # aggregated (positions)
├── catalog/glue.json          # 48 tables: hedge_bronze (25), hedge_silver (22), hedge_gold (1)
└── scripts/
    ├── provision.py           # sample market_ticks/orders/ohlcv/positions
    ├── provision-floci.py     # Floci mode: S3 buckets hedge-bronze/silver/gold + Glue DBs + Firehose
    └── sync-worldbank-to-floci.py / sync-*-to-floci.py # boto3 upload endpoint http://localhost:4566
```

## Catalog (excerpt)

- `hedge_bronze.worldbank_raw` `s3://hedge-bronze/worldbank/` `LocalPath data/bronze/worldbank/` cols `indicator_id, country_iso3, date, value...`
- `hedge_silver.worldbank_observations` `s3://hedge-silver/worldbank/` `LocalPath data/silver/worldbank/worldbank_observations/` 15 cols
- `hedge_bronze.yahoo_raw` `s3://hedge-bronze/yahoo/` `symbol, date, open...`, `hedge_silver.yahoo_ohlcv` `symbol,date,epoch,open...adj_close,volume`
- Generic: `hedge_bronze.{src}_raw` `s3://hedge-bronze/{src}/` `source_key, raw`, `hedge_silver.{src}` `source_key, raw_len, bronze_path` for 20 sources (cboe, investing, tencent, sina, eastmoney, baostock, binance, coinbase, defillama, fred, treasury, sec, imf, oecd, calcfi, fdic, eia, bls, bea, gmd)

## Query (DuckDB = Athena)

```java
var lake = Datalake.defaultLocal(); // finds datalake/ from any subproject
lake.loadCatalog().databases(); // hedge_bronze, hedge_silver, hedge_gold
try (var qe = new QueryEngine()) {
  qe.query("SELECT count(*) FROM read_csv('datalake/data/silver/yahoo/yahoo_ohlcv.csv', header=true)");
  qe.query("SELECT * FROM read_csv('datalake/data/silver/worldbank/worldbank_observations/observations.csv', header=true) WHERE indicator_id='SP.POP.TOTL' LIMIT 5");
  qe.query("SELECT source_key, raw_len FROM read_csv('datalake/data/silver/binance/binance.csv', header=true)");
}
```

CLI:
```bash
python3 datalake/scripts/provision.py
./gradlew :libs:datalake:run --args="query \"SELECT symbol, avg(price) FROM read_csv('datalake/data/bronze/market_ticks/*.csv', header=true) GROUP BY symbol\""
./gradlew :libs:datalake:test # QueryEngine 200 ticks verified
```

## Floci Mode (Docker)

```bash
floci start                         # container bf... Up healthy, floci doctor All checks passed
eval $(floci env)                   # AWS_ENDPOINT_URL=http://localhost:4566
python3 datalake/scripts/provision-floci.py # S3 buckets + Glue
aws s3 ls s3://hedge-bronze/ --endpoint-url $AWS_ENDPOINT_URL
python3 datalake/scripts/sync-worldbank-to-floci.py # uploads bronze/silver
```

`.gitignore` bulk: `datalake/data/bronze/{worldbank,yahoo,cboe,...}/` + `silver/...` ignored, catalog tracked.

## Writes

- **Bronze:** `BronzeWriter.write(key, raw)` → `data/bronze/{src}/key={safe}/data.raw` (or `symbol=.../data.ndjson`, `indicator=batch_XXXX/date=`) atomic `tmp`→`move`.
- **Silver:** `SilverTransformer.transform` → `data/silver/{src}/{src}.csv` (dedup `symbol|date` for yahoo, `indicator|country|date` for worldbank, generic `source_key,raw_len` for others).
- **Watermark:** `bronze/_watermark.json` `{"lastRun":"2026-...","keys":N}`.

See `libs/datalake/src/main/java/com/hedgefund/datalake/Datalake.java:1` `QueryEngine.java:1`.
