#!/usr/bin/env python3
"""Parallel sync of bronze/silver/gold to Floci S3 (boto3).

- 16-way parallel uploads (was serial upload_file loop)
- Skips unchanged files via local manifest (size+mtime), so re-runs are incremental
- Covers per-source dirs + partitioned market/macro + single-file parquets + gold
- Same buckets/endpoint/creds as before (Floci-compatible)
"""
import concurrent.futures
import json
import os
import pathlib

import boto3
from boto3.s3.transfer import TransferConfig
from botocore.config import Config

endpoint = os.getenv("AWS_ENDPOINT_URL", "http://localhost:4566")
WORKERS = int(os.getenv("FLOCI_SYNC_WORKERS", "16"))
root = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = root / "data" / ".floci_manifest.json"

s3 = boto3.client(
    "s3",
    endpoint_url=endpoint,
    region_name="us-east-1",
    aws_access_key_id="test",
    aws_secret_access_key="test",
    config=Config(signature_version="s3v4"),
)
TRANSFER = TransferConfig(
    multipart_threshold=8 * 1024 * 1024,
    max_concurrency=8,
    multipart_chunksize=8 * 1024 * 1024,
    use_threads=True,
)

for b in ["hedge-bronze", "hedge-silver", "hedge-gold"]:
    try:
        s3.create_bucket(Bucket=b)
    except Exception:
        pass


def load_manifest():
    if MANIFEST.exists():
        try:
            return json.loads(MANIFEST.read_text())
        except Exception:
            return {}
    return {}


def collect():
    jobs = []  # (local_path, bucket, key)
    for src in ["worldbank", "yahoo", "cboe", "investing", "tencent", "sina",
                "eastmoney", "baostock", "binance", "coinbase", "defillama", "fred",
                "treasury", "sec", "imf", "oecd", "calcfi", "fdic", "eia", "bls",
                "bea", "gmd", "market_ticks", "orders"]:
        bronze = root / "data" / "bronze" / src
        if bronze.exists():
            for f in bronze.rglob("*"):
                if f.is_file():
                    jobs.append((f, "hedge-bronze", f"{src}/{f.relative_to(bronze)}"))
        silver = root / "data" / "silver" / src
        if silver.exists():
            for f in silver.rglob("*"):
                if f.is_file() and f.suffix != ".tmp":
                    jobs.append((f, "hedge-silver", f"{src}/{f.relative_to(silver)}"))
    # columnar silver: partitioned dirs + single-file parquets
    for extra in ["market", "macro"]:
        d = root / "data" / "silver" / extra
        if d.exists():
            for f in d.rglob("*.parquet"):
                jobs.append((f, "hedge-silver", f"{extra}/{f.relative_to(d)}"))
    for pq in (root / "data" / "silver").glob("*.parquet"):
        jobs.append((pq, "hedge-silver", pq.name))
    gold = root / "data" / "gold"
    if gold.exists():
        for f in gold.rglob("*"):
            if f.is_file() and f.suffix != ".tmp":
                jobs.append((f, "hedge-gold", str(f.relative_to(gold))))
    return jobs


def main():
    manifest = load_manifest()
    jobs = collect()
    todo = []
    skipped = 0
    for local, bucket, key in jobs:
        st = local.stat()
        sig = f"{st.st_size}:{int(st.st_mtime)}"
        if manifest.get(f"{bucket}/{key}") == sig:
            skipped += 1
            continue
        todo.append((local, bucket, key, sig))

    uploaded = 0

    def put(job):
        local, bucket, key, sig = job
        s3.upload_file(str(local), bucket, key, Config=TRANSFER)
        return f"{bucket}/{key}", sig

    with concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS) as ex:
        for dest, sig in ex.map(put, todo):
            manifest[dest] = sig
            uploaded += 1
            if uploaded % 200 == 0:
                print(f"  ...{uploaded}/{len(todo)} uploaded")

    MANIFEST.write_text(json.dumps(manifest))
    print(f"uploaded {uploaded}, skipped unchanged {skipped}, total {len(jobs)}")
    print("buckets", [b["Name"] for b in s3.list_buckets().get("Buckets", [])])


if __name__ == "__main__":
    main()
