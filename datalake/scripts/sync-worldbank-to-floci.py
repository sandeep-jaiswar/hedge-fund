#!/usr/bin/env python3
"""Sync World Bank bronze/silver local files to Floci LocalStack S3 (when floci running)."""
import pathlib, boto3, os
from botocore.config import Config

endpoint = os.getenv("AWS_ENDPOINT_URL", "http://localhost:4566")
s3 = boto3.client("s3", endpoint_url=endpoint, region_name="us-east-1",
                  aws_access_key_id="test", aws_secret_access_key="test",
                  config=Config(signature_version="s3v4"))

root = pathlib.Path(__file__).resolve().parents[1]
bronze = root / "data/bronze/worldbank"
silver = root / "data/silver/worldbank"

def sync(local: pathlib.Path, bucket: str, prefix: str):
    if not local.exists():
        print(f"skip {local} not exists")
        return
    for f in local.rglob("*"):
        if f.is_file():
            key = prefix + str(f.relative_to(local))
            s3.upload_file(str(f), bucket, key)
            print(f"uploaded s3://{bucket}/{key} ({f.stat().st_size} bytes)")

for b, bucket, prefix in [(bronze, "hedge-bronze", "worldbank/"), (silver, "hedge-silver", "worldbank/")]:
    sync(b, bucket, prefix)

print("sync done, buckets:", [b["Name"] for b in s3.list_buckets().get("Buckets",[])])
