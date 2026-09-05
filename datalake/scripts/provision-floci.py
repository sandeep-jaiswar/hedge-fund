#!/usr/bin/env python3
"""
Provision same hedge-fund datalake to Floci (requires `floci start` / Docker).
Creates S3 buckets, Glue databases/tables, and optionally Firehose streams.
Uses boto3 pointed at http://localhost:4566. Idempotent.
"""
import sys, boto3, botocore
from pathlib import Path

ENDPOINT = "http://localhost:4566"
REGION = "us-east-1"
KW = dict(endpoint_url=ENDPOINT, region_name=REGION, aws_access_key_id="test", aws_secret_access_key="test")

def ensure_bucket(s3, name):
    try:
        s3.create_bucket(Bucket=name)
        print(f"Created bucket {name}")
    except botocore.exceptions.ClientError as e:
        if "BucketAlreadyOwnedByYou" in str(e) or "BucketAlreadyExists" in str(e):
            print(f"Bucket {name} exists")
        else:
            raise

def ensure_db(glue, name, desc, loc):
    try:
        glue.create_database(DatabaseInput={"Name": name, "Description": desc, "LocationUri": loc})
        print(f"Created DB {name}")
    except glue.exceptions.AlreadyExistsException:
        print(f"DB {name} exists")
    except Exception as e:
        if "AlreadyExists" in str(e):
            print(f"DB {name} exists")
        else:
            raise

def ensure_table(glue, db, name, loc, cols, input_fmt="org.apache.hadoop.mapred.TextInputFormat"):
    try:
        glue.create_table(DatabaseName=db, TableInput={
            "Name": name,
            "StorageDescriptor": {
                "Location": loc,
                "InputFormat": input_fmt,
                "OutputFormat": "org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat",
                "SerdeInfo": {"SerializationLibrary": "org.openx.data.jsonserde.JsonSerDe"},
                "Columns": cols
            }
        })
        print(f"Created table {db}.{name}")
    except Exception as e:
        if "AlreadyExists" in str(e):
            print(f"Table {db}.{name} exists")
        else:
            print(f"Table create failed {db}.{name}: {e}")

def main():
    try:
        import boto3
    except ImportError:
        print("boto3 not installed: pip install boto3")
        sys.exit(1)

    s3 = boto3.client("s3", **KW, config=botocore.config.Config(s3={"addressing_style": "path"}))
    glue = boto3.client("glue", **KW)
    # health check
    try:
        s3.list_buckets()
    except Exception as e:
        print(f"Floci not reachable at {ENDPOINT}: {e}\nRun `floci start` first (requires Docker).")
        sys.exit(1)

    for b in ["hedge-bronze", "hedge-silver", "hedge-gold", "floci-firehose-results"]:
        ensure_bucket(s3, b)

    ensure_db(glue, "hedge_bronze", "raw ticks/orders", "s3://hedge-bronze/")
    ensure_db(glue, "hedge_silver", "cleaned ohlcv", "s3://hedge-silver/")
    ensure_db(glue, "hedge_gold", "gold portfolio", "s3://hedge-gold/")

    ensure_table(glue, "hedge_bronze", "market_ticks", "s3://hedge-bronze/market_ticks/",
                 [{"Name":"symbol","Type":"string"},{"Name":"ts","Type":"bigint"},{"Name":"price","Type":"double"},{"Name":"volume","Type":"double"},{"Name":"exchange","Type":"string"}])
    ensure_table(glue, "hedge_bronze", "orders", "s3://hedge-bronze/orders/",
                 [{"Name":"id","Type":"int"},{"Name":"symbol","Type":"string"},{"Name":"side","Type":"string"},{"Name":"qty","Type":"int"},{"Name":"price","Type":"double"}])
    ensure_table(glue, "hedge_silver", "ohlcv", "s3://hedge-silver/ohlcv/",
                 [{"Name":"symbol","Type":"string"},{"Name":"window_start","Type":"string"},{"Name":"open","Type":"double"},{"Name":"high","Type":"double"},{"Name":"low","Type":"double"},{"Name":"close","Type":"double"},{"Name":"volume","Type":"bigint"}])
    ensure_table(glue, "hedge_gold", "positions", "s3://hedge-gold/positions/",
                 [{"Name":"symbol","Type":"string"},{"Name":"qty","Type":"int"},{"Name":"avg_price","Type":"double"},{"Name":"market_price","Type":"double"},{"Name":"pnl","Type":"double"}])

    # upload local data to Floci S3 if provision.py already ran
    root = Path(__file__).parents[1]
    import os
    for local, bucket, key in [
        (root/"data/bronze/market_ticks/market_ticks.csv", "hedge-bronze", "market_ticks/market_ticks.csv"),
        (root/"data/bronze/orders/orders.ndjson", "hedge-bronze", "orders/orders.ndjson"),
        (root/"data/silver/ohlcv/ohlcv.csv", "hedge-silver", "ohlcv/ohlcv.csv"),
        (root/"data/gold/positions/positions.csv", "hedge-gold", "positions/positions.csv"),
    ]:
        if local.exists():
            s3.upload_file(str(local), bucket, key)
            print(f"Uploaded {local.name} -> s3://{bucket}/{key}")

    print("Floci datalake provisioned. Try: aws glue get-databases --endpoint-url http://localhost:4566")

if __name__ == "__main__":
    main()
