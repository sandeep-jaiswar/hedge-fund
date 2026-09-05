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
                try (var qe = new QueryEngine()) {
                    List<Map<String, Object>> rows = qe.query(sql);
                    rows.forEach(System.out::println);
                    System.out.println("-- " + rows.size() + " rows");
                }
            }
            default -> System.out.println("Unknown command: " + args[0]);
        }
    }
}
