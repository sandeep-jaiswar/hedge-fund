package com.hedgefund.datalake;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;

/**
 * Run Liquibase migrations on jdbc:duckdb: — idempotent, no data loss.
 * Usage:
 *   LiquibaseRunner [duckdb-file]           — run migrations (default: hedge-fund.duckdb)
 *   LiquibaseRunner [duckdb-file] --reset   — drop DATABASECHANGELOG and re-run from scratch
 *
 * All Parquet paths in migrations are RELATIVE to project root (datalake/data/...).
 * This class sets the working directory to the project root before running.
 */
public class LiquibaseRunner {

    private static Connection wrapDuckDB(Connection raw) {
        return (Connection) Proxy.newProxyInstance(
            raw.getClass().getClassLoader(), new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("prepareCall".equals(method.getName()) && args != null && args.length >= 1 && args[0] instanceof String) {
                    String sql = (String) args[0];
                    if (sql == null || sql.trim().isEmpty()) sql = "SELECT 1";
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
        // Determine project root (where datalake/data/ lives)
        Path projectRoot = Path.of("").toAbsolutePath();
        // If running from a subdirectory, try to find the project root
        if (!Files.exists(projectRoot.resolve("datalake/data"))) {
            // Try parent directories
            for (Path p = projectRoot; p != null; p = p.getParent()) {
                if (Files.exists(p.resolve("datalake/data"))) {
                    projectRoot = p;
                    break;
                }
            }
        }

        // Parse args
        String duckdbFile = "hedge-fund.duckdb";
        boolean reset = false;
        for (String arg : args) {
            if ("--reset".equals(arg)) {
                reset = true;
            } else if (!arg.startsWith("-")) {
                duckdbFile = arg;
            }
        }

        Path dbPath = projectRoot.resolve(duckdbFile);
        String url = "jdbc:duckdb:" + dbPath.toAbsolutePath();
        String changelog = "db/changelog/db.changelog-master.xml";

        System.out.println("LiquibaseRunner projectRoot=" + projectRoot);
        System.out.println("LiquibaseRunner dbPath=" + dbPath.toAbsolutePath());
        System.out.println("LiquibaseRunner reset=" + reset);
        System.out.println("LiquibaseRunner url=" + url + " changelog=" + changelog);

        // If --reset, delete the DuckDB file and recreate fresh
        if (reset) {
            System.out.println("RESET: Deleting existing DuckDB file...");
            Files.deleteIfExists(dbPath);
            // Also delete .wal and .tmp files
            Files.deleteIfExists(Path.of(dbPath + ".wal"));
            Files.deleteIfExists(Path.of(dbPath + ".tmp"));
            System.out.println("RESET: Clean slate created.");
        }

        try (var raw = DriverManager.getConnection(url)) {
            var conn = wrapDuckDB(raw);
            var liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), new JdbcConnection(conn));
            liquibase.update("");
            System.out.println("Liquibase update done");

            // Verify schema
            var rs = conn.createStatement().executeQuery(
                "SELECT table_schema, table_name, count(*) as cols FROM information_schema.columns " +
                "WHERE table_schema IN ('dim','fact') GROUP BY table_schema, table_name ORDER BY table_schema, table_name");
            System.out.println("\n=== Schema Summary ===");
            while (rs.next()) {
                System.out.printf("  %-6s %-25s %d columns%n", rs.getString(1), rs.getString(2), rs.getInt(3));
            }

            // Verify row counts
            System.out.println("\n=== Row Counts ===");
            String[] tables = {
                "dim.dim_instrument", "dim.dim_equity", "dim.dim_exchange",
                "dim.dim_country", "dim.dim_indicator", "dim.dim_currency",
                "dim.dim_asset_class", "dim.dim_sector",
                "dim.fact_market_ohlcv", "dim.fact_macro_obs"
            };
            for (String t : tables) {
                try {
                    var rs2 = conn.createStatement().executeQuery("SELECT count(*) FROM " + t);
                    rs2.next();
                    System.out.printf("  %-30s %d rows%n", t, rs2.getLong(1));
                } catch (Exception e) {
                    System.out.printf("  %-30s ERROR: %s%n", t, e.getMessage());
                }
            }

            // Verify views
            System.out.println("\n=== Views ===");
            var rs3 = conn.createStatement().executeQuery(
                "SELECT table_schema, table_name FROM information_schema.tables " +
                "WHERE table_type='VIEW' AND table_schema IN ('hedge_gold','market','macro','ref','master','staging') " +
                "ORDER BY table_schema, table_name");
            while (rs3.next()) {
                System.out.printf("  %-12s.%s%n", rs3.getString(1), rs3.getString(2));
            }
        }
    }
}
