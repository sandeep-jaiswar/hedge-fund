#!/usr/bin/env python3
"""Sync all bronze/silver/gold to Floci S3 (boto3) — covers 22 sources + worldbank + yahoo + gold."""
import pathlib, boto3, os
from botocore.config import Config
endpoint = os.getenv("AWS_ENDPOINT_URL", "http://localhost:4566")
s3 = boto3.client("s3", endpoint_url=endpoint, region_name="us-east-1", aws_access_key_id="test", aws_secret_access_key="test", config=Config(signature_version="s3v4"))
root = pathlib.Path(__file__).resolve().parents[1]
# ensure buckets exist
for b in ["hedge-bronze","hedge-silver","hedge-gold"]:
    try: s3.create_bucket(Bucket=b)
    except: pass
def sync(local: pathlib.Path, bucket: str, prefix: str):
    if not local.exists():
        print(f"skip {local}")
        return 0
    n=0
    for f in local.rglob("*"):
        if f.is_file():
            key = prefix + str(f.relative_to(local))
            s3.upload_file(str(f), bucket, key)
            n+=1
    print(f"synced {local} -> s3://{bucket}/{prefix} {n} files")
    return n
total=0
for src in ["worldbank","yahoo","cboe","investing","tencent","sina","eastmoney","baostock","binance","coinbase","defillama","fred","treasury","sec","imf","oecd","calcfi","fdic","eia","bls","bea","gmd","market_ticks","orders"]:
    total+=sync(root/"data/bronze"/src, "hedge-bronze", f"{src}/")
    total+=sync(root/"data/silver"/src, "hedge-silver", f"{src}/")
# gold
total+=sync(root/"data/gold", "hedge-gold", "")
# catalog
print("buckets", [b["Name"] for b in s3.list_buckets().get("Buckets",[])])
print(f"total synced files ~{total}")
