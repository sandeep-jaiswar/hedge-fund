package com.hedgefund.datalake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Local Athena equivalent using DuckDB JDBC — no Docker, no Floci.
 * Can query CSV/NDJSON/Parquet directly in datalake/data/**.
 * When Floci is running, same SQL works against DuckDB views that Floci generates from Glue+S3.
 */
public class QueryEngine implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(QueryEngine.class);
    private final Connection conn;

    public QueryEngine() throws SQLException {
        this(null);
    }

    /**
     * Open a persistent DuckDB file when given (thin Parquet views from 011),
     * otherwise fall back to in-memory. Use {@link #defaultFile()} to locate
     * the repo-root hedge-fund.duckdb from any subproject.
     */
    public QueryEngine(java.nio.file.Path dbFile) throws SQLException {
        try {
            Class.forName("org.duckdb.DuckDBDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("DuckDB driver not found", e);
        }
        if (dbFile != null) {
            this.conn = DriverManager.getConnection("jdbc:duckdb:" + dbFile.toAbsolutePath());
            log.info("DuckDB file engine started: {}", dbFile.toAbsolutePath());
        } else {
            this.conn = DriverManager.getConnection("jdbc:duckdb:");
            log.info("DuckDB in-memory engine started (Athena local mock)");
        }
    }

    /** Locate hedge-fund.duckdb by walking up from CWD (mirrors Datalake.defaultLocal). */
    public static java.nio.file.Path defaultFile() {
        java.nio.file.Path cwd = java.nio.file.Path.of(System.getProperty("user.dir", "")).toAbsolutePath().normalize();
        for (java.nio.file.Path cur = cwd; cur != null; cur = cur.getParent()) {
            java.nio.file.Path candidate = cur.resolve("hedge-fund.duckdb");
            if (java.nio.file.Files.exists(candidate)) return candidate;
            if (java.nio.file.Files.exists(cur.resolve("datalake/catalog/glue.json"))) {
                java.nio.file.Path rootCandidate = cur.resolve("hedge-fund.duckdb");
                if (java.nio.file.Files.exists(rootCandidate)) return rootCandidate;
            }
        }
        return null;
    }

    /** Execute SQL and return rows as List<Map<String,Object>> (like Athena GetQueryResults) */
    public synchronized List<Map<String, Object>> query(String sql) throws SQLException {
        log.info("Query: {}", sql);
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
            log.info("Query returned {} rows", rows.size());
            return rows;
        }
    }

    /** Convenience: query CSV directly - escapes single quotes to prevent SQL injection */
    public List<Map<String, Object>> queryCsv(String csvPath, String sqlWhere) throws SQLException {
        String escaped = csvPath.replace("'", "''");
        String sql = "SELECT * FROM read_csv('" + escaped + "', header=true) " + (sqlWhere != null ? sqlWhere : "");
        return query(sql);
    }

    /** Convenience: query Parquet directly (single file, glob, or hive-partitioned dir). */
    public List<Map<String, Object>> queryParquet(String parquetGlob, String sqlWhere) throws SQLException {
        String escaped = parquetGlob.replace("'", "''");
        boolean hive = parquetGlob.contains("=") || parquetGlob.contains("**");
        String fn = hive
            ? "read_parquet('" + escaped + "', hive_partitioning=true)"
            : "read_parquet('" + escaped + "')";
        return query("SELECT * FROM " + fn + " " + (sqlWhere != null ? sqlWhere : ""));
    }

    private static volatile QueryEngine shared;

    /**
     * Shared file-backed engine over the repo-root hedge-fund.duckdb (thin Parquet views).
     * Synchronized single connection: safe for concurrent callers, avoids per-query startup.
     */
    public static QueryEngine shared() throws SQLException {
        QueryEngine qe = shared;
        if (qe != null) return qe;
        synchronized (QueryEngine.class) {
            if (shared != null) return shared;
            java.nio.file.Path db = defaultFile();
            if (db == null) throw new SQLException("hedge-fund.duckdb not found; run from the repo");
            shared = new QueryEngine(db);
            return shared;
        }
    }

    @Override
    public void close() throws SQLException {
        conn.close();
        log.info("DuckDB closed");
    }

    // Simple demo
    public static void main(String[] args) throws Exception {
        String sql = args.length > 0 ? String.join(" ", args) : "SELECT 1 as hello";
        try (var qe = new QueryEngine()) {
            var rows = qe.query(sql);
            rows.forEach(System.out::println);
        }
    }
}
