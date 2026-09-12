#!/usr/bin/env python3
"""
Generate catalog/glue.json from the current DuckDB schema.
Replaces the hardcoded provision.py catalog with a live schema reflection.
"""
import json
import duckdb
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DB_PATH = ROOT.parent / "hedge-fund.duckdb"
CATALOG = ROOT / "catalog/glue.json"

SKIP_SCHEMAS = {"pg_catalog", "information_schema", "main"}
SKIP_TABLES = {"DATABASECHANGELOG", "DATABASECHANGELOGLOCK"}

DUCKDB_TO_GLUE = {
    "VARCHAR": "string",
    "INTEGER": "int",
    "BIGINT": "bigint",
    "SMALLINT": "smallint",
    "TINYINT": "tinyint",
    "DOUBLE": "double",
    "FLOAT": "float",
    "BOOLEAN": "boolean",
    "DATE": "date",
    "TIMESTAMP": "timestamp",
    "TIMESTAMPTZ": "timestamp",
    "BLOB": "binary",
    "UUID": "string",
}

SCHEMA_DESCRIPTIONS = {
    "dim": "Dimension and fact tables (3NF)",
    "fact": "Fact views (read-only projections)",
    "ref": "Reference dimension views",
    "master": "Master instrument/equity views",
    "market": "Market domain views (ohlcv, crypto, filings)",
    "macro": "Macro-economic views (rates, observations)",
    "hedge_bronze": "Raw ingested data (NDJSON/CSV)",
    "hedge_silver": "Cleaned and typed silver-layer data",
    "hedge_gold": "Gold-layer aggregations and analytics",
    "staging": "Staging views for ingestion pipeline",
}


def map_type(duckdb_type: str) -> str:
    upper = duckdb_type.upper().strip()
    if "INT" in upper:
        if "BIG" in upper:
            return "bigint"
        if "SMALL" in upper:
            return "smallint"
        if "TINY" in upper:
            return "tinyint"
        return "int"
    if upper in ("DOUBLE", "FLOAT", "DECIMAL", "NUMERIC", "HUGEINT"):
        return "double"
    if upper == "BOOLEAN":
        return "boolean"
    if "DATE" in upper and "TIME" not in upper:
        return "date"
    if "TIMESTAMP" in upper:
        return "timestamp"
    if upper == "BLOB":
        return "binary"
    if upper == "UUID":
        return "string"
    return "string"


def schema_to_db(schema: str) -> str:
    return schema


def schema_to_s3(schema: str) -> str:
    if schema.startswith("hedge_"):
        return f"s3://hedge-{schema[6:]}/"
    return f"s3://hedge-{schema}/"


def main():
    if not DB_PATH.exists():
        print(f"ERROR: DuckDB not found at {DB_PATH}")
        return 1

    con = duckdb.connect(str(DB_PATH), read_only=True)

    rows = con.execute("""
        SELECT table_schema, table_name, column_name, data_type
        FROM information_schema.columns
        WHERE table_schema NOT IN ('pg_catalog', 'information_schema', 'main')
          AND table_name NOT IN ('DATABASECHANGELOG', 'DATABASECHANGELOGLOCK')
        ORDER BY table_schema, table_name, ordinal_position
    """).fetchall()
    con.close()

    tables_map = {}
    for schema, table, col_name, col_type in rows:
        key = (schema, table)
        if key not in tables_map:
            tables_map[key] = []
        tables_map[key].append({"Name": col_name, "Type": map_type(col_type)})

    schemas_seen = sorted(set(s for s, _ in tables_map.keys()))

    databases = []
    for schema in schemas_seen:
        databases.append({
            "Name": schema_to_db(schema),
            "Description": SCHEMA_DESCRIPTIONS.get(schema, f"Schema: {schema}"),
            "LocationUri": schema_to_s3(schema)
        })

    tables = []
    for (schema, table), columns in sorted(tables_map.items()):
        db_name = schema_to_db(schema)
        tables.append({
            "DatabaseName": db_name,
            "Name": table,
            "StorageDescriptor": {
                "Location": schema_to_s3(schema) + table + "/",
                "Columns": columns
            },
            "LocalPath": f"data/{schema}/{table}/"
        })

    catalog = {"databases": databases, "tables": tables}

    CATALOG.parent.mkdir(parents=True, exist_ok=True)
    with open(CATALOG, "w") as f:
        json.dump(catalog, f, indent=2)

    print(f"Generated {CATALOG}")
    print(f"  {len(databases)} databases: {', '.join(d['Name'] for d in databases)}")
    print(f"  {len(tables)} tables across {len(schemas_seen)} schemas")
    for schema in schemas_seen:
        count = sum(1 for s, _ in tables_map.keys() if s == schema)
        print(f"    {schema}: {count} tables")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
