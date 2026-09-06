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
        try {
            Class.forName("org.duckdb.DuckDBDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("DuckDB driver not found", e);
        }
        this.conn = DriverManager.getConnection("jdbc:duckdb:");
        log.info("DuckDB in-memory engine started (Athena local mock)");
    }

    /** Execute SQL and return rows as List<Map<String,Object>> (like Athena GetQueryResults) */
    public List<Map<String, Object>> query(String sql) throws SQLException {
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
