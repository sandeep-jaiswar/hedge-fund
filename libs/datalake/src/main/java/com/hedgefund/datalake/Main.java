package com.hedgefund.datalake;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * CLI for local datalake: provision | query | catalog
 * Keeps it SIMPLE — no Spring, plain Java 21.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: provision | query \"SQL\" | catalog");
            return;
        }
        var lake = Datalake.defaultLocal();
        switch (args[0]) {
            case "migrate" -> {
                // Usage: migrate [--reset] [duckdb-file]
                String duckdbFile = "hedge-fund.duckdb";
                boolean reset = false;
                for (int i = 1; i < args.length; i++) {
                    if ("--reset".equals(args[i])) reset = true;
                    else if (!args[i].startsWith("-")) duckdbFile = args[i];
                }
                var runnerArgs = new java.util.ArrayList<String>();
                runnerArgs.add(duckdbFile);
                if (reset) runnerArgs.add("--reset");
                LiquibaseRunner.main(runnerArgs.toArray(new String[0]));
                return;
            }
            case "provision" -> {
                lake.provisionSampleData();
                System.out.println("Provisioned at " + lake.getRoot());
            }
            case "catalog" -> {
                var cat = lake.loadCatalog();
                System.out.println("Databases:");
                cat.databases().forEach(db -> System.out.println(" - " + db.Name() + " -> " + db.LocationUri()));
                System.out.println("Tables:");
                cat.tables().forEach(t -> System.out.println(" - " + t.DatabaseName() + "." + t.Name() + " -> " + t.StorageDescriptor().Location()));
            }
            case "query" -> {
                String sql = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "SELECT 1";
                String dbFile = "hedge-fund.duckdb";
                // Check if first extra arg is a db file
                for (int i = 1; i < args.length; i++) {
                    if (args[i].endsWith(".duckdb")) { dbFile = args[i]; break; }
                }
                java.nio.file.Path dbPath = java.nio.file.Path.of(dbFile);
                if (java.nio.file.Files.exists(dbPath)) {
                    try (var conn = java.sql.DriverManager.getConnection("jdbc:duckdb:" + dbPath.toAbsolutePath())) {
                        var rs = conn.createStatement().executeQuery(sql);
                        var meta = rs.getMetaData();
                        int cols = meta.getColumnCount();
                        while (rs.next()) {
                            var sb = new StringBuilder();
                            for (int i = 1; i <= cols; i++) {
                                if (i > 1) sb.append(" | ");
                                sb.append(meta.getColumnName(i)).append("=").append(rs.getObject(i));
                            }
                            System.out.println(sb);
                        }
                        rs.close();
                    }
                } else {
                    try (var qe = new QueryEngine()) {
                        List<Map<String, Object>> rows = qe.query(sql);
                        rows.forEach(System.out::println);
                        System.out.println("-- " + rows.size() + " rows");
                    }
                }
            }
            default -> System.out.println("Unknown command: " + args[0]);
        }
    }
}
