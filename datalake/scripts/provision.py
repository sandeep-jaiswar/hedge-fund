#!/usr/bin/env python3
"""
Provision local hedge-fund datalake sample data — no AWS/Floci/Docker needed.
Generates CSV + NDJSON that mimics Firehose -> S3 -> Glue -> Athena flow.
Idempotent: overwrites existing files.
"""
import csv, json, random
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BRONZE_TICKS = ROOT / "data/bronze/market_ticks"
BRONZE_ORDERS = ROOT / "data/bronze/orders"
SILVER_OHLCV = ROOT / "data/silver/ohlcv"
GOLD_POSITIONS = ROOT / "data/gold/positions"
CATALOG = ROOT / "catalog/glue.json"

SYMBOLS = ["AAPL", "MSFT", "GOOG", "TSLA", "SPY"]
random.seed(42)

def ensure_dirs():
    for p in [BRONZE_TICKS, BRONZE_ORDERS, SILVER_OHLCV, GOLD_POSITIONS, CATALOG.parent]:
        p.mkdir(parents=True, exist_ok=True)

def gen_market_ticks():
    path = BRONZE_TICKS / "market_ticks.csv"
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["symbol","ts","price","volume","exchange"])
        ts = 1700000000000
        for _ in range(200):
            sym = random.choice(SYMBOLS)
            price = round(random.uniform(100, 500), 2)
            vol = random.randint(100, 10000)
            ex = random.choice(["NYSE","NASDAQ"])
            w.writerow([sym, ts, price, vol, ex])
            ts += random.randint(100, 1000)
    print(f"Wrote {path} (200 rows)")

    # also write NDJSON variant (Firehose raw) for Floci compatibility
    nd = BRONZE_TICKS / "market_ticks.ndjson"
    with open(path) as csvf, open(nd, "w") as out:
        reader = csv.DictReader(csvf)
        for row in reader:
            json.dump(row, out)
            out.write("\n")
    print(f"Wrote {nd}")

def gen_orders():
    path = BRONZE_ORDERS / "orders.json"
    orders = []
    for i in range(1, 51):
        orders.append({
            "id": i,
            "symbol": random.choice(SYMBOLS),
            "side": random.choice(["BUY","SELL"]),
            "qty": random.randint(1, 500),
            "price": round(random.uniform(100, 500), 2),
            "ts": 1700000000000 + i*60000
        })
    with open(path, "w") as f:
        json.dump(orders, f, indent=2)
    # NDJSON for Firehose S3
    nd = BRONZE_ORDERS / "orders.ndjson"
    with open(nd, "w") as f:
        for o in orders:
            json.dump(o, f)
            f.write("\n")
    print(f"Wrote {path} and {nd} (50 rows)")

def gen_ohlcv():
    path = SILVER_OHLCV / "ohlcv.csv"
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["symbol","window_start","open","high","low","close","volume"])
        for sym in SYMBOLS:
            o = round(random.uniform(100, 300), 2)
            for wday in range(5):
                high = round(o * random.uniform(1, 1.02), 2)
                low = round(o * random.uniform(0.98, 1), 2)
                close = round(random.uniform(low, high), 2)
                vol = random.randint(100000, 1000000)
                w.writerow([sym, f"2024-01-0{wday+1}T09:30:00Z", o, high, low, close, vol])
                o = close
    print(f"Wrote {path} (25 rows)")

def gen_positions():
    path = GOLD_POSITIONS / "positions.csv"
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["symbol","qty","avg_price","market_price","pnl"])
        for sym in SYMBOLS:
            qty = random.randint(100, 1000)
            avg = round(random.uniform(150, 350), 2)
            mkt = round(avg * random.uniform(0.95, 1.05), 2)
            pnl = round((mkt - avg) * qty, 2)
            w.writerow([sym, qty, avg, mkt, pnl])
    print(f"Wrote {path} (5 rows)")

def write_catalog():
    catalog = {
        "databases": [
            {"Name": "hedge_bronze", "Description": "raw ticks/orders (Floci Glue)", "LocationUri": "s3://hedge-bronze/"},
            {"Name": "hedge_silver", "Description": "cleaned ohlcv", "LocationUri": "s3://hedge-silver/"},
            {"Name": "hedge_gold", "Description": "gold portfolio/risk", "LocationUri": "s3://hedge-gold/"}
        ],
        "tables": [
            {"DatabaseName": "hedge_bronze", "Name": "market_ticks", "StorageDescriptor": {"Location": "s3://hedge-bronze/market_ticks/", "InputFormat": "org.apache.hadoop.mapred.TextInputFormat", "SerdeInfo": {"SerializationLibrary": "org.openx.data.jsonserde.JsonSerDe"}, "Columns": [{"Name":"symbol","Type":"string"},{"Name":"ts","Type":"bigint"},{"Name":"price","Type":"double"},{"Name":"volume","Type":"double"},{"Name":"exchange","Type":"string"}]}, "LocalPath": "data/bronze/market_ticks/"},
            {"DatabaseName": "hedge_bronze", "Name": "orders", "StorageDescriptor": {"Location": "s3://hedge-bronze/orders/", "InputFormat": "org.apache.hadoop.mapred.TextInputFormat", "Columns": [{"Name":"id","Type":"int"},{"Name":"symbol","Type":"string"},{"Name":"side","Type":"string"},{"Name":"qty","Type":"int"},{"Name":"price","Type":"double"}]}, "LocalPath": "data/bronze/orders/"},
            {"DatabaseName": "hedge_silver", "Name": "ohlcv", "StorageDescriptor": {"Location": "s3://hedge-silver/ohlcv/", "Columns": [{"Name":"symbol","Type":"string"},{"Name":"window_start","Type":"string"},{"Name":"open","Type":"double"},{"Name":"high","Type":"double"},{"Name":"low","Type":"double"},{"Name":"close","Type":"double"},{"Name":"volume","Type":"bigint"}]}, "LocalPath": "data/silver/ohlcv/"},
            {"DatabaseName": "hedge_gold", "Name": "positions", "StorageDescriptor": {"Location": "s3://hedge-gold/positions/", "Columns": [{"Name":"symbol","Type":"string"},{"Name":"qty","Type":"int"},{"Name":"avg_price","Type":"double"},{"Name":"market_price","Type":"double"},{"Name":"pnl","Type":"double"}]}, "LocalPath": "data/gold/positions/"}
        ]
    }
    with open(CATALOG, "w") as f:
        json.dump(catalog, f, indent=2)
    print(f"Wrote {CATALOG}")

if __name__ == "__main__":
    ensure_dirs()
    gen_market_ticks()
    gen_orders()
    gen_ohlcv()
    gen_positions()
    write_catalog()
    print("Done. Local datalake provisioned without Docker/Floci.")
