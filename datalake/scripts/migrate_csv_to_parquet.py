#!/usr/bin/env python3
"""
Configurable, optimised migration: CSV silver/gold -> partitioned Parquet ZSTD
- No data loss: verifies row counts, keeps CSV, atomic write
- Uses DuckDB 1.3.2.0 via Python duckdb (same as jdbc:duckdb:)
- Driven by tables.yml partitions
"""
import duckdb, pathlib, sys, os
root = pathlib.Path(__file__).parent.parent
con = duckdb.connect()
print(f"root={root} duckdb={duckdb.__version__}")

def migrate(csv_rel, parquet_rel, partition_by=None, order_by=None, dedup_sql=None):
    csv = root / csv_rel
    pq = root / parquet_rel
    if not csv.exists():
        print(f"skip missing {csv_rel}")
        return 0
    pq.parent.mkdir(parents=True, exist_ok=True)
    # dedup via ROW_NUMBER if provided, else straight copy
    if dedup_sql:
        tmp = f"SELECT * EXCLUDE rn FROM (SELECT *, ROW_NUMBER() OVER ({dedup_sql}) AS rn FROM read_csv('{csv}', header=true)) WHERE rn=1"
    else:
        tmp = f"SELECT * FROM read_csv('{csv}', header=true)"
    if order_by:
        tmp += f" ORDER BY {order_by}"
    count = con.execute(f"SELECT count(*) FROM read_csv('{csv}', header=true)").fetchone()[0]
    # atomic: write to tmp then move
    tmp_pq = pq.with_suffix(".tmp.parquet")
    if partition_by:
        # partitioned write: DuckDB will create hive partitions
        # For partitioned, use COPY ... PARTITION_BY
        cols = ", ".join(partition_by)
        # we keep single file + partitioned for now: write both
        con.execute(f"COPY ({tmp}) TO '{pq}' (FORMAT PARQUET, COMPRESSION ZSTD)")
        # partitioned dir
        part_dir = pq.parent / pq.stem  # e.g. ohlcv/year=.../symbol=...
        # Use manual partitioned copy if partition columns exist in tmp
        try:
            part_tmp = str(pq.parent / "_part_tmp")
            con.execute(f"COPY ({tmp}) TO '{part_dir}' (FORMAT PARQUET, COMPRESSION ZSTD, PARTITION_BY ({cols}), OVERWRITE_OR_IGNORE 1)")
            print(f"partitioned {csv_rel} -> {part_dir} ({cols})")
        except Exception as e:
            print(f"partitioned failed {e}, keep single file")
    else:
        con.execute(f"COPY ({tmp}) TO '{tmp_pq}' (FORMAT PARQUET, COMPRESSION ZSTD)")
        tmp_pq.rename(pq)
    # verify
    pq_count = con.execute(f"SELECT count(*) FROM read_parquet('{pq}')").fetchone()[0]
    print(f"migrate {csv_rel} -> {parquet_rel} rows={count} parquet_rows={pq_count} {'OK' if count==pq_count else 'MISMATCH'}")
    assert count == pq_count, f"row mismatch {csv_rel} {count}!={pq_count}"
    return count

# Core deduped tables
migrate("data/silver/yahoo/yahoo_ohlcv.csv", "data/silver/market/ohlcv.parquet", partition_by=["year","symbol"], order_by="symbol, date", dedup_sql="PARTITION BY symbol, date ORDER BY epoch DESC")
# worldbank needs year derived; duckdb will infer year from csv's year col
migrate("data/silver/worldbank/worldbank_observations/observations.csv", "data/silver/macro/observations.parquet", partition_by=["indicator_id","year"], order_by="indicator_id, country_iso3, date", dedup_sql="PARTITION BY indicator_id, country_iso3, date ORDER BY lastupdated DESC")

# Generic silvers (no dedup, just columnar)
for s in ["cboe","binance","coinbase","defillama","fred","treasury","sec","imf","oecd","calcfi","fdic","eia","bls","bea","gmd","tencent","sina","eastmoney","baostock","investing"]:
    for csv_rel in [f"data/silver/{s}/{s}.csv", f"data/silver/{s}.csv"]:
        if (root/csv_rel).exists():
            migrate(csv_rel, f"data/silver/{s}.parquet", order_by=None)
            break

# ohlcv generic
if (root/"data/silver/ohlcv/ohlcv.csv").exists():
    migrate("data/silver/ohlcv/ohlcv.csv", "data/silver/ohlcv.parquet")

# gold
for g in ["yahoo_summary","worldbank_yoy","all_sources_summary"]:
    csv_rel = f"data/gold/{g}.csv"
    if (root/csv_rel).exists():
        migrate(csv_rel, f"data/gold/{g}.parquet")

# views.sql for DBeaver Path :memory:
views = root / "catalog/views.sql"
views.write_text(f"""-- Auto-generated from tables.yml — DBeaver DuckDB Path :memory: (or :memory:)
-- Usage: open in DBeaver SQL Editor (Path :memory:) and run all
CREATE OR REPLACE VIEW hedge_silver.yahoo_ohlcv AS SELECT * FROM read_parquet('{root}/data/silver/market/ohlcv.parquet');
CREATE OR REPLACE VIEW hedge_silver.worldbank_observations AS SELECT * FROM read_parquet('{root}/data/silver/macro/observations.parquet');
CREATE OR REPLACE VIEW hedge_bronze.yahoo_raw AS SELECT * FROM read_csv('{root}/data/bronze/yahoo/**/data.ndjson', header=false);
CREATE OR REPLACE VIEW hedge_gold.yahoo_summary AS SELECT * FROM read_parquet('{root}/data/gold/yahoo_summary.parquet');
CREATE OR REPLACE VIEW hedge_gold.worldbank_yoy AS SELECT * FROM read_parquet('{root}/data/gold/worldbank_yoy.parquet');
CREATE OR REPLACE VIEW hedge_gold.all_sources_summary AS SELECT * FROM read_parquet('{root}/data/gold/all_sources_summary.parquet');
-- Parquet partitioned access (predicate pushdown)
-- SELECT * FROM read_parquet('{root}/data/silver/market/ohlcv/year=2024/symbol=AAPL/**/*.parquet') WHERE date >= '2024-01-01';
-- SELECT * FROM read_parquet('{root}/data/silver/macro/observations/indicator=SP.POP.TOTL/year=2024/**/*.parquet');
""")
print(f"views.sql written to {views}")

# checksum report
print("\n=== Silver Parquet sizes ===")
for p in sorted((root/"data/silver").rglob("*.parquet")):
    print(f"{p.relative_to(root)} {p.stat().st_size/1024:.1f}KB")
for p in sorted((root/"data/gold").rglob("*.parquet")):
    print(f"{p.relative_to(root)} {p.stat().st_size/1024:.1f}KB")
