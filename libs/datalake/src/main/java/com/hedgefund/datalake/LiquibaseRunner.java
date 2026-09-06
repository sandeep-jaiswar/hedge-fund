package com.hedgefund.datalake;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import java.lang.reflect.*;
import java.sql.*;

/** Run Liquibase migrations on jdbc:duckdb: file — idempotent, no data loss.
 * DuckDB JDBC does not support prepareCall -> wrap to delegate to prepareStatement. */
public class LiquibaseRunner {
    private static Connection wrapDuckDB(Connection raw) {
        return (Connection) Proxy.newProxyInstance(
            raw.getClass().getClassLoader(), new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("prepareCall".equals(method.getName()) && args != null && args.length >= 1 && args[0] instanceof String) {
                    String sql = (String) args[0];
                    if (sql == null || sql.trim().isEmpty()) sql = "SELECT 1";
                    // DuckDB does not support FOR UPDATE / stored procedures — strip and delegate to prepareStatement
                    if (sql.toUpperCase().contains("FOR UPDATE")) sql = sql.replaceAll("(?i)\\s+FOR\\s+UPDATE", "");
                    if (sql.toUpperCase().contains("FOR SHARE")) sql = sql.replaceAll("(?i)\\s+FOR\\s+SHARE", "");
                    if (args.length == 1) return raw.prepareStatement(sql);
                    if (args.length == 3) return raw.prepareStatement(sql, (Integer) args[1], (Integer) args[2]);
                    return raw.prepareStatement(sql);
                }
                try { return method.invoke(raw, args); }
                catch (InvocationTargetException e) { throw e.getCause(); }
            });
    }
    public static void main(String[] args) throws Exception {
        String url = args.length>0 ? args[0] : "jdbc:duckdb:/media/sandeep/DataDrive2/hedge-fund/hedge-fund.duckdb";
        String changelog = "db/changelog/db.changelog-master.xml";
        System.out.println("Liquibase update url=" + url + " changelog=" + changelog);
        try (var raw = DriverManager.getConnection(url)) {
            var conn = wrapDuckDB(raw);
            var liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), new JdbcConnection(conn));
            liquibase.update("");
            System.out.println("Liquibase update done");
            var rs = conn.createStatement().executeQuery("SELECT count(*) FROM dim.dim_instrument");
            rs.next(); System.out.println("dim_instrument=" + rs.getLong(1));
            rs = conn.createStatement().executeQuery("SELECT count(*) FROM fact.fact_market_ohlcv");
            rs.next(); System.out.println("fact_ohlcv=" + rs.getLong(1));
            rs = conn.createStatement().executeQuery("SELECT count(*) FROM dim.dim_indicator");
            rs.next(); System.out.println("dim_indicator=" + rs.getLong(1));
        }
    }
}
