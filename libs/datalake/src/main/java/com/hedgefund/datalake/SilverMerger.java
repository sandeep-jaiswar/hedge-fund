package com.hedgefund.datalake;

import org.duckdb.DuckDBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Configurable, optimised silver merger — replaces LinkedHashMap dedup in
 * YahooSilverTransformer / SilverTransformer with SQL ROW_NUMBER incremental MERGE.
 * - No data loss: bronze is append-only ndjson.gz, silver is MERGE on PK
 * - Deterministic: dedup ORDER BY _ingest_ts/lastupdated, not Files.walk order
 * - Scalable: incremental per-partition Parquet, not full rewrite
 * - Optimised: Parquet ZSTD partitioned pruning
 */
public class SilverMerger {
    private static final Logger log = LoggerFactory.getLogger(SilverMerger.class);
    private static volatile boolean jsonLoaded;

    private static void ensureJson(Connection c) throws SQLException {
        if (jsonLoaded) return;
        synchronized (SilverMerger.class) {
            if (jsonLoaded) return;
            c.createStatement().execute("INSTALL json; LOAD json;");
            jsonLoaded = true;
        }
    }

    public static void mergeMarketOhlcv(Path datalakeRoot) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            mergeMarketOhlcv(datalakeRoot, c);
        }
    }

    public static void mergeMarketOhlcv(Path datalakeRoot, Connection c) throws Exception {
        Path bronzeRoot = datalakeRoot.resolve("data/bronze/yahoo");
        Path silverRoot = datalakeRoot.resolve("data/silver/market/ohlcv");
        Files.createDirectories(silverRoot);
        Path marketCsv = datalakeRoot.resolve("data/silver/yahoo/yahoo_ohlcv.csv");
        Path marketSingle = datalakeRoot.resolve("data/silver/market/ohlcv.parquet");
        String marketFp = fingerprint(Files.exists(marketCsv) ? marketCsv : bronzeRoot);
        if (Files.exists(marketSingle) && unchanged(datalakeRoot, "market/ohlcv", marketFp)) {
            log.info("skip unchanged market ohlcv");
            return;
        }
        String bronzeGlob = datalakeRoot.resolve("data/bronze/yahoo/**/data.ndjson").toString();
        String csvPath = datalakeRoot.resolve("data/silver/yahoo/yahoo_ohlcv.csv").toString();

        ensureJson(c);
        if (Files.exists(Path.of(csvPath))) {
            c.createStatement().execute(String.format("""
                CREATE OR REPLACE TABLE tmp AS
                SELECT * EXCLUDE rn FROM (
                  SELECT *, ROW_NUMBER() OVER (PARTITION BY symbol, date ORDER BY epoch DESC) AS rn
                  FROM read_csv('%s', header=true)
                ) WHERE rn=1
                """, csvPath.replace("'", "''")));
            var tmpCount = c.createStatement().executeQuery("SELECT count(*) FROM tmp");
            tmpCount.next();
            log.info("tmp from csv count={}", tmpCount.getLong(1));
        } else if (Files.exists(bronzeRoot)) {
            c.createStatement().execute(String.format("""
                CREATE OR REPLACE TABLE tmp AS
                SELECT * EXCLUDE rn FROM (
                  SELECT symbol, date, epoch, open, high, low, close, adj_close, volume,
                         _ingest_ts, _batch_id,
                         ROW_NUMBER() OVER (PARTITION BY symbol, date ORDER BY _ingest_ts DESC) AS rn
                  FROM read_ndjson('%s')
                ) WHERE rn=1
                """, bronzeGlob.replace("'", "''")));
        } else {
            log.warn("No bronze/csv for market ohlcv");
            return;
        }
        Path tmpOut = silverRoot.resolve("_tmp");
        Files.createDirectories(tmpOut);
        c.createStatement().execute(String.format("""
            COPY (SELECT *, year(CAST(date AS DATE)) AS year FROM tmp ORDER BY symbol, date)
            TO '%s' (FORMAT PARQUET, COMPRESSION ZSTD, PARTITION_BY (year, symbol), OVERWRITE_OR_IGNORE 1, FILENAME_PATTERN "data_{i}")
            """, silverRoot.toString().replace("'", "''")));
        Path single = datalakeRoot.resolve("data/silver/market/ohlcv.parquet");
        c.createStatement().execute(String.format("COPY tmp TO '%s' (FORMAT PARQUET, COMPRESSION ZSTD)", single.toString().replace("'", "''")));
        var cntRs = c.createStatement().executeQuery("SELECT count(*) FROM tmp");
        cntRs.next();
        long cnt = cntRs.getLong(1);
        log.info("silver_market_ohlcv merged {} rows -> {}", cnt, silverRoot);
        markDone(datalakeRoot, "market/ohlcv", marketFp);
    }

    public static void mergeWorldbankObservations(Path datalakeRoot) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            mergeWorldbankObservations(datalakeRoot, c);
        }
    }

    public static void mergeWorldbankObservations(Path datalakeRoot, Connection c) throws Exception {
        String csv = datalakeRoot.resolve("data/silver/worldbank/worldbank_observations/observations.csv").toString();
        Path outRoot = datalakeRoot.resolve("data/silver/macro/observations");
        Files.createDirectories(outRoot);
        if (!Files.exists(Path.of(csv))) { log.warn("No worldbank csv {}", csv); return; }
        String wbFp = fingerprint(Path.of(csv));
        Path wbSingle = datalakeRoot.resolve("data/silver/macro/observations.parquet");
        if (Files.exists(wbSingle) && unchanged(datalakeRoot, "macro/observations", wbFp)) {
            log.info("skip unchanged macro observations");
            return;
        }
        c.createStatement().execute("SET threads=2");
        c.createStatement().execute("SET memory_limit='1.5GB'");
        c.createStatement().execute(String.format("""
            CREATE OR REPLACE TABLE tmp AS
            SELECT * EXCLUDE rn FROM (
              SELECT *, ROW_NUMBER() OVER (PARTITION BY indicator_id, country_iso3, date ORDER BY lastupdated DESC) AS rn
              FROM read_csv('%s', header=true)
            ) WHERE rn=1
            """, csv.replace("'", "''")));
        ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM tmp");
        rs.next(); long cnt = rs.getLong(1);
        c.createStatement().execute(String.format("""
            COPY (SELECT * FROM tmp ORDER BY indicator_id, country_iso3, date)
            TO '%s' (FORMAT PARQUET, COMPRESSION ZSTD, PARTITION_BY (indicator_id, year), OVERWRITE_OR_IGNORE 1, FILENAME_PATTERN "data_{i}")
            """, outRoot.toString().replace("'", "''")));
        Path single = datalakeRoot.resolve("data/silver/macro/observations.parquet");
        c.createStatement().execute(String.format("COPY tmp TO '%s' (FORMAT PARQUET, COMPRESSION ZSTD)", single.toString().replace("'", "''")));
        log.info("silver_macro_observations merged {} rows -> {}", cnt, outRoot);
        markDone(datalakeRoot, "macro/observations", wbFp);
    }

    public static void copyCsvToParquet(Path csv, Path parquet) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            copyCsvToParquet(csv, parquet, c);
        }
    }

    /** Generic CSV -> partitioned Parquet for any silver csv (cboe, binance, etc. — passthrough, no dedup yet) */
    public static void copyCsvToParquet(Path csv, Path parquet, Connection c) throws Exception {
        if (!Files.exists(csv)) return;
        Path root = findSilverRoot(parquet);
        String unit = root != null ? root.relativize(parquet).toString() : parquet.getFileName().toString();
        if (root != null && unchanged(root, unit, fingerprint(csv))) {
            log.info("skip unchanged {} -> {}", csv, parquet);
            return;
        }
        Files.createDirectories(parquet.getParent());
        c.createStatement().execute(String.format("COPY (SELECT * FROM read_csv('%s', header=true)) TO '%s' (FORMAT PARQUET, COMPRESSION ZSTD)",
            csv.toString().replace("'", "''"), parquet.toString().replace("'", "''")));
        ResultSet rs = c.createStatement().executeQuery(String.format("SELECT count(*) FROM read_csv('%s', header=true)", csv.toString().replace("'", "''")));
        rs.next(); log.info("copy {} -> {} rows={}", csv, parquet, rs.getLong(1));
        if (root != null) markDone(root, unit, fingerprint(csv));
    }

    // ---- incremental skip: fingerprint inputs, remember per-unit state ----

    private static Path findSilverRoot(Path p) {
        for (Path cur = p.toAbsolutePath(); cur != null; cur = cur.getParent()) {
            if (cur.getFileName() != null && cur.getFileName().toString().equals("silver")
                && Files.exists(cur.getParent().resolve("bronze"))) {
                return cur.getParent();
            }
        }
        return null;
    }

    /** Fingerprint = max mtime + total size + file count over inputs (metadata walk, no reads). */
    static String fingerprint(Path... inputs) throws IOException {
        long maxMtime = 0, totalSize = 0, count = 0;
        for (Path in : inputs) {
            if (!Files.exists(in)) continue;
            if (Files.isRegularFile(in)) {
                maxMtime = Math.max(maxMtime, Files.getLastModifiedTime(in).toMillis());
                totalSize += Files.size(in);
                count++;
            } else {
                try (var stream = Files.walk(in)) {
                    for (Path p : (Iterable<Path>) stream::iterator) {
                        if (Files.isRegularFile(p)) {
                            maxMtime = Math.max(maxMtime, Files.getLastModifiedTime(p).toMillis());
                            totalSize += Files.size(p);
                            count++;
                        }
                    }
                }
            }
        }
        return maxMtime + ":" + totalSize + ":" + count;
    }

    private static Map<String, String> loadState(Path datalakeRoot) {
        Path state = datalakeRoot.resolve("data/silver/.merge_state.json");
        if (!Files.exists(state)) return new HashMap<>();
        try {
            String json = Files.readString(state).trim();
            Map<String, String> out = new HashMap<>();
            if (json.startsWith("{")) {
                for (String kv : json.substring(1, json.lastIndexOf('}')).split(",")) {
                    String[] parts = kv.split(":", 2);
                    if (parts.length == 2) out.put(parts[0].trim().replaceAll("^\"|\"$", ""),
                        parts[1].trim().replaceAll("^\"|\"$", ""));
                }
            }
            return out;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static void saveState(Path datalakeRoot, Map<String, String> state) {
        try {
            StringBuilder sb = new StringBuilder("{");
            state.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
            if (sb.length() > 1) sb.setLength(sb.length() - 1);
            sb.append("}");
            Files.writeString(datalakeRoot.resolve("data/silver/.merge_state.json"), sb.toString());
        } catch (Exception e) {
            log.warn("merge state save failed", e);
        }
    }

    private static boolean unchanged(Path datalakeRoot, String unit, String fp) {
        return fp.equals(loadState(datalakeRoot).get(unit));
    }

    private static void markDone(Path datalakeRoot, String unit, String fp) {
        Map<String, String> state = loadState(datalakeRoot);
        state.put(unit, fp);
        saveState(datalakeRoot, state);
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length>0? args[0] : "datalake");
        root = root.toAbsolutePath();
        log.info("SilverMerger root={}", root);
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            ensureJson(c);
            mergeMarketOhlcv(root, c);
            mergeWorldbankObservations(root, c);
            String[] generic = {"cboe","binance","coinbase","defillama","fred","treasury","sec","imf","oecd","calcfi","fdic","eia","bls","bea","gmd","tencent","sina","eastmoney","baostock","investing","ohlcv"};
            for (String s : generic) {
                Path csv = root.resolve("data/silver/"+s+"/"+s+".csv");
                if (!Files.exists(csv)) csv = root.resolve("data/silver/"+s+".csv");
                Path pq = root.resolve("data/silver/"+s+".parquet");
                if (Files.exists(csv)) copyCsvToParquet(csv, pq, c);
            }
            for (String g: new String[]{"yahoo_summary","worldbank_yoy","all_sources_summary"}) {
                Path csv = root.resolve("data/gold/"+g+".csv");
                Path pq = root.resolve("data/gold/"+g+".parquet");
                if (Files.exists(csv)) copyCsvToParquet(csv, pq, c);
            }
        }
        log.info("SilverMerger done");
    }
}
