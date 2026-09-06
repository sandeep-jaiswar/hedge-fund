# Deployment — Floci 2.0.1 + LocalStack

Floci server is Docker-only (`floci/floci:latest`), CLI `~/.local/bin/floci` orchestrator, endpoint `http://localhost:4566/_floci/health` (`floci doctor` 11 checks, `container bf... Up healthy`).

## Start Floci

```bash
sudo floci start                        # or FLOCI_SUDO_PASSWORD=xxx sudo -S floci start, Docker 29.7.2 exposes 4566
floci doctor                            # All checks passed
docker ps                               # 0.0.0.0:4566->4566
eval $(floci env)                       # AWS_ENDPOINT_URL=http://localhost:4566 AWS_ACCESS_KEY_ID=test
```

## Provision

```bash
python3 datalake/scripts/provision.py               # local sample
python3 datalake/scripts/provision-floci.py          # S3 buckets hedge-bronze/silver/gold + Glue hedge_bronze/silver/gold + Firehose floci-firehose-results
python3 -c "import boto3; from botocore.config import Config; s3=boto3.client('s3', endpoint_url='http://localhost:4566', region_name='us-east-1', aws_access_key_id='test', aws_secret_access_key='test', config=Config(signature_version='s3v4')); print(s3.list_buckets())"
```

Buckets verified `hedge-bronze, hedge-silver, hedge-gold` via `boto3`.

## Sync Datalake to Floci S3

```bash
python3 datalake/scripts/sync-worldbank-to-floci.py  # uploads data/bronze/worldbank + data/silver/worldbank/worldbank_observations/observations.csv (584494 bytes)
python3 datalake/scripts/sync-yahoo-to-floci.py      # pattern for each src: for f in data/bronze/{src}/rglob; s3.upload_file(f, bucket, prefix+rel)
# generic
for src in yahoo cboe binance coinbase defillama tencent baostock investing sina eastmoney fred treasury sec imf oecd calcfi fdic eia bls bea gmd worldbank; do
  python3 datalake/scripts/sync-${src}-to-floci.py 2>&1 | tail
done
```

Check:

```bash
aws s3 ls s3://hedge-bronze/worldbank/ --endpoint-url http://localhost:4566
aws s3 ls s3://hedge-silver/yahoo/ --endpoint-url http://localhost:4566
aws glue get-databases --endpoint-url http://localhost:4566
aws athena start-query-execution --query-string "SELECT count(*) FROM hedge_silver.yahoo_ohlcv" --endpoint-url http://localhost:4566
```

## Datalake Mapping

| Local | S3 URI | Glue DB | Table |
|---|---|---|---|
| `data/bronze/worldbank/` | `s3://hedge-bronze/worldbank/` | `hedge_bronze` | `worldbank_raw` |
| `data/silver/worldbank/worldbank_observations/` | `s3://hedge-silver/worldbank/` | `hedge_silver` | `worldbank_observations` |
| `data/bronze/yahoo/` | `s3://hedge-bronze/yahoo/` | `hedge_bronze` | `yahoo_raw` |
| `data/silver/yahoo/` | `s3://hedge-silver/yahoo/` | `hedge_silver` | `yahoo_ohlcv` |
| ... 20 more `s3://hedge-bronze/{src}/` / `s3://hedge-silver/{src}/` | | | |

See `datalake/catalog/glue.json:1` (48 tables).

## Stop

```bash
floci stop
docker ps
```

No local Gradle install needed; `sudo` via `FLOCI_SUDO_PASSWORD` env or `flociSudoPassword` gradle property.
