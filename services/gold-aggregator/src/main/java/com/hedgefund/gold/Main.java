package com.hedgefund.gold;
import com.hedgefund.datalake.Datalake;
import java.nio.file.*;
import java.sql.*;
public class Main {
    public static void main(String[] args) throws Exception {
        Path root = Datalake.defaultLocal().getRoot();
        Path goldRoot = root.resolve("data/gold");
        Files.createDirectories(goldRoot);
        int generated = 0;
        int skipped = 0;
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            // Yahoo OHLCV summary — prefer Parquet, fallback to CSV
            Path yahooPq = root.resolve("data/silver/market/ohlcv.parquet");
            Path yahooCsv = root.resolve("data/silver/yahoo/yahoo_ohlcv.csv");
            if (Files.exists(yahooPq)) {
                generated += runGold(c, "yahoo_summary.csv", goldRoot,
                    "SELECT symbol, count(*) as bars, avg(close) as avg_close, max(high) as max_high, min(low) as min_low, sum(volume) as total_vol FROM read_parquet('" + yahooPq + "') GROUP BY symbol");
            } else if (Files.exists(yahooCsv)) {
                generated += runGold(c, "yahoo_summary.csv", goldRoot,
                    "SELECT symbol, count(*) as bars, avg(close) as avg_close, max(high) as max_high, min(low) as min_low, sum(volume) as total_vol FROM read_csv('" + yahooCsv + "', header=true) GROUP BY symbol");
            }
            // World Bank YoY — prefer Parquet, fallback to CSV
            Path wbPq = root.resolve("data/silver/macro/observations.parquet");
            Path wbCsv = root.resolve("data/silver/worldbank/worldbank_observations/observations.csv");
            if (Files.exists(wbPq)) {
                generated += runGold(c, "worldbank_yoy.csv", goldRoot,
                    "SELECT indicator_id, country_iso3, date, value, lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date) as prev, (value - lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date))/nullif(lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date),0) as yoy FROM read_parquet('" + wbPq + "') WHERE indicator_id='SP.POP.TOTL' LIMIT 1000");
            } else if (Files.exists(wbCsv)) {
                generated += runGold(c, "worldbank_yoy.csv", goldRoot,
                    "SELECT indicator_id, country_iso3, date, value, lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date) as prev, (value - lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date))/nullif(lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date),0) as yoy FROM read_csv('" + wbCsv + "', header=true) WHERE indicator_id='SP.POP.TOTL' LIMIT 1000");
            } else { skipped++; }
            // Treasury yield curve
            Path treasuryPq = root.resolve("data/silver/treasury.parquet");
            Path treasuryCsv = root.resolve("data/silver/treasury/treasury.csv");
            if (Files.exists(treasuryPq) && hasColumnPq(c, treasuryPq, "Date")) {
                generated += runGold(c, "treasury_yield_curve.csv", goldRoot,
                    "SELECT * FROM read_parquet('" + treasuryPq + "') ORDER BY \"Date\"");
            } else if (Files.exists(treasuryCsv) && hasColumn(c, treasuryCsv, "Date")) {
                generated += runGold(c, "treasury_yield_curve.csv", goldRoot,
                    "SELECT * FROM read_csv('" + treasuryCsv + "', header=true) ORDER BY \"Date\"");
            } else {
                System.out.println("SKIP treasury_yield_curve: no typed data available");
                skipped++;
            }
            // FRED real rates
            Path fredPq = root.resolve("data/silver/fred.parquet");
            Path fredCsv = root.resolve("data/silver/fred/fred.csv");
            if (Files.exists(fredPq) && hasColumnPq(c, fredPq, "date")) {
                generated += runGold(c, "fred_real_rates.csv", goldRoot,
                    "SELECT * FROM read_parquet('" + fredPq + "') ORDER BY date");
            } else if (Files.exists(fredCsv) && hasColumn(c, fredCsv, "date")) {
                generated += runGold(c, "fred_real_rates.csv", goldRoot,
                    "SELECT * FROM read_csv('" + fredCsv + "', header=true) ORDER BY date");
            } else {
                System.out.println("SKIP fred_real_rates: no typed data available");
                skipped++;
            }
            // EIA oil prices
            Path eiaPq = root.resolve("data/silver/eia.parquet");
            Path eiaCsv = root.resolve("data/silver/eia/eia.csv");
            if (Files.exists(eiaPq) && hasColumnPq(c, eiaPq, "date")) {
                generated += runGold(c, "eia_oil_prices.csv", goldRoot,
                    "SELECT * FROM read_parquet('" + eiaPq + "') ORDER BY date");
            } else if (Files.exists(eiaCsv) && hasColumn(c, eiaCsv, "date")) {
                generated += runGold(c, "eia_oil_prices.csv", goldRoot,
                    "SELECT * FROM read_csv('" + eiaCsv + "', header=true) ORDER BY date");
            } else {
                System.out.println("SKIP eia_oil_prices: no typed data available");
                skipped++;
            }
            // SEC filings
            Path secPq = root.resolve("data/silver/sec.parquet");
            Path secCsv = root.resolve("data/silver/sec/sec.csv");
            if (Files.exists(secPq) && hasColumnPq(c, secPq, "date")) {
                generated += runGold(c, "sec_filings.csv", goldRoot,
                    "SELECT * FROM read_parquet('" + secPq + "')");
            } else if (Files.exists(secCsv) && hasColumn(c, secCsv, "date")) {
                generated += runGold(c, "sec_filings.csv", goldRoot,
                    "SELECT * FROM read_csv('" + secCsv + "', header=true)");
            } else {
                System.out.println("SKIP sec_filings: no typed data available");
                skipped++;
            }
        }
        // Generic gold: count per source using watermarks (fast, no file walking)
        {
            Path outAll = goldRoot.resolve("all_sources_summary.csv");
            StringBuilder sb = new StringBuilder("source,bronze_keys,silver_rows\n");
            for (String src : new String[]{"yahoo","binance","coinbase","defillama","worldbank","treasury","sec","fred","cboe","investing","tencent","sina","eastmoney","baostock","imf","oecd","calcfi","fdic","eia","bls","bea","gmd"}) {
                long b = readWatermarkCount(root.resolve("data/bronze/" + src));
                long s = countSilverRows(root, src);
                sb.append(src).append(",").append(b).append(",").append(s).append("\n");
            }
            Files.writeString(outAll, sb.toString());
            System.out.println("Gold all_sources_summary -> "+outAll);
            generated++;
        }
        System.out.println("Gold aggregation done. Generated=" + generated + " Skipped=" + skipped + " Root=" + goldRoot);
    }

    private static long readWatermarkCount(Path bronzeDir) {
        Path wm = bronzeDir.resolve("_watermark.json");
        if (!Files.exists(wm)) return 0;
        try {
            String json = Files.readString(wm).trim();
            int idx = json.indexOf("\"symbols\":");
            if (idx < 0) return 0;
            String num = json.substring(idx + 10).replaceAll("[^0-9].*", "");
            return num.isEmpty() ? 0 : Long.parseLong(num);
        } catch (Exception e) { return 0; }
    }

    private static long countSilverRows(Path root, String src) {
        Path pq = root.resolve("data/silver/" + src + ".parquet");
        if (Files.exists(pq)) {
            try (var c = DriverManager.getConnection("jdbc:duckdb:");
                 var rs = c.createStatement().executeQuery("SELECT count(*) FROM read_parquet('" + pq + "')")) {
                rs.next(); return rs.getLong(1);
            } catch (Exception e) { return 0; }
        }
        Path csv = root.resolve("data/silver/" + src + "/" + src + ".csv");
        if (!Files.exists(csv)) csv = root.resolve("data/silver/" + src + ".csv");
        if (Files.exists(csv)) {
            try { return Files.lines(csv).count() - 1; } catch (Exception e) { return 0; }
        }
        return 0;
    }

    /** Check if a CSV file has a specific column header */
    private static boolean hasColumn(Connection c, Path csvPath, String column) throws SQLException {
        if (!Files.exists(csvPath)) return false;
        try (var rs = c.createStatement().executeQuery(
            "SELECT column_name FROM (DESCRIBE SELECT * FROM read_csv('" + csvPath + "', header=true)) WHERE column_name='" + column + "'")) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    /** Check if a Parquet file has a specific column */
    private static boolean hasColumnPq(Connection c, Path pqPath, String column) throws SQLException {
        if (!Files.exists(pqPath)) return false;
        try (var rs = c.createStatement().executeQuery(
            "SELECT column_name FROM (DESCRIBE SELECT * FROM read_parquet('" + pqPath + "')) WHERE column_name='" + column + "'")) {
            return rs.next();
        } catch (Exception e) {
            return false;
        }
    }

    /** Execute a gold query and write output. Returns 1 on success, 0 on skip/failure. */
    private static int runGold(Connection c, String outName, Path goldRoot, String sql) {
        try {
            Path out = goldRoot.resolve(outName);
            c.createStatement().execute("COPY (" + sql + ") TO '" + out.toString() + "' (HEADER, DELIMITER ',')");
            System.out.println("Gold " + outName + " -> " + out);
            return 1;
        } catch (Exception e) {
            System.out.println("SKIP " + outName + ": " + e.getMessage());
            return 0;
        }
    }
}
