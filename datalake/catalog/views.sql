-- Auto-generated from tables.yml — DBeaver DuckDB Path :memory: (or :memory:)
-- Usage: open in DBeaver SQL Editor (Path :memory:) and run all
CREATE OR REPLACE VIEW hedge_silver.yahoo_ohlcv AS SELECT * FROM read_parquet('/media/sandeep/DataDrive2/hedge-fund/datalake/data/silver/market/ohlcv.parquet');
CREATE OR REPLACE VIEW hedge_silver.worldbank_observations AS SELECT * FROM read_parquet('/media/sandeep/DataDrive2/hedge-fund/datalake/data/silver/macro/observations.parquet');
CREATE OR REPLACE VIEW hedge_bronze.yahoo_raw AS SELECT * FROM read_csv('/media/sandeep/DataDrive2/hedge-fund/datalake/data/bronze/yahoo/**/data.ndjson', header=false);
CREATE OR REPLACE VIEW hedge_gold.yahoo_summary AS SELECT * FROM read_parquet('/media/sandeep/DataDrive2/hedge-fund/datalake/data/gold/yahoo_summary.parquet');
CREATE OR REPLACE VIEW hedge_gold.worldbank_yoy AS SELECT * FROM read_parquet('/media/sandeep/DataDrive2/hedge-fund/datalake/data/gold/worldbank_yoy.parquet');
CREATE OR REPLACE VIEW hedge_gold.all_sources_summary AS SELECT * FROM read_parquet('/media/sandeep/DataDrive2/hedge-fund/datalake/data/gold/all_sources_summary.parquet');
-- Parquet partitioned access (predicate pushdown)
-- SELECT * FROM read_parquet('/media/sandeep/DataDrive2/hedge-fund/datalake/data/silver/market/ohlcv/year=2024/symbol=AAPL/**/*.parquet') WHERE date >= '2024-01-01';
-- SELECT * FROM read_parquet('/media/sandeep/DataDrive2/hedge-fund/datalake/data/silver/macro/observations/indicator=SP.POP.TOTL/year=2024/**/*.parquet');
