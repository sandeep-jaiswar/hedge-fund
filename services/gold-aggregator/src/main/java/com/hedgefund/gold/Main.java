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
            // Yahoo OHLCV summary
            generated += runGold(c, "yahoo_summary.csv", goldRoot,
                "SELECT symbol, count(*) as bars, avg(close) as avg_close, max(high) as max_high, min(low) as min_low, sum(volume) as total_vol FROM read_csv('" + root.resolve("data/silver/yahoo/yahoo_ohlcv.csv") + "', header=true) GROUP BY symbol");
            // World Bank YoY
            Path wbSilver = root.resolve("data/silver/worldbank/worldbank_observations/observations.csv");
            if (Files.exists(wbSilver)) {
                generated += runGold(c, "worldbank_yoy.csv", goldRoot,
                    "SELECT indicator_id, country_iso3, date, value, lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date) as prev, (value - lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date))/nullif(lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date),0) as yoy FROM read_csv('" + wbSilver + "', header=true) WHERE indicator_id='SP.POP.TOTL' LIMIT 1000");
            } else { skipped++; }
            // Treasury yield curve - check if typed columns exist
            Path treasurySilver = root.resolve("data/silver/treasury/treasury.csv");
            if (hasColumn(c, treasurySilver, "Date")) {
                generated += runGold(c, "treasury_yield_curve.csv", goldRoot,
                    "SELECT * FROM read_csv('" + treasurySilver + "', header=true) ORDER BY \"Date\"");
            } else {
                System.out.println("SKIP treasury_yield_curve: silver has generic columns only (needs typed silver parser)");
                skipped++;
            }
            // FRED real rates - check if typed columns exist
            Path fredSilver = root.resolve("data/silver/fred/fred.csv");
            if (hasColumn(c, fredSilver, "date")) {
                generated += runGold(c, "fred_real_rates.csv", goldRoot,
                    "SELECT * FROM read_csv('" + fredSilver + "', header=true) ORDER BY date");
            } else {
                System.out.println("SKIP fred_real_rates: silver has generic columns only (needs typed silver parser)");
                skipped++;
            }
            // EIA oil prices
            Path eiaSilver = root.resolve("data/silver/eia/eia.csv");
            if (hasColumn(c, eiaSilver, "date")) {
                generated += runGold(c, "eia_oil_prices.csv", goldRoot,
                    "SELECT * FROM read_csv('" + eiaSilver + "', header=true) ORDER BY date");
            } else {
                System.out.println("SKIP eia_oil_prices: silver has generic columns only (needs typed silver parser)");
                skipped++;
            }
            // SEC filings
            Path secSilver = root.resolve("data/silver/sec/sec.csv");
            if (hasColumn(c, secSilver, "date")) {
                generated += runGold(c, "sec_filings.csv", goldRoot,
                    "SELECT * FROM read_csv('" + secSilver + "', header=true)");
            } else {
                System.out.println("SKIP sec_filings: silver has generic columns only (needs typed silver parser)");
                skipped++;
            }
        }
        // Generic gold: count per source (file-based, no DuckDB needed)
        {
            Path outAll = goldRoot.resolve("all_sources_summary.csv");
            StringBuilder sb = new StringBuilder("source,bronze_keys,silver_rows\n");
            for (String src : new String[]{"yahoo","binance","coinbase","defillama","worldbank","treasury","sec","fred","cboe","investing","tencent","sina","eastmoney","baostock","imf","oecd","calcfi","fdic","eia","bls","bea","gmd"}) {
                Path bronze = root.resolve("data/bronze/"+src);
                Path silver = root.resolve("data/silver/"+src);
                long b = 0;
                if (Files.exists(bronze)) {
                    try { b = Files.walk(bronze).filter(p->p.getFileName().toString().equals("data.raw")||p.getFileName().toString().equals("data.ndjson")).count(); } catch(Exception e){}
                }
                long s = 0;
                if (Files.exists(silver)) {
                    try { s = Files.walk(silver).filter(p->p.toString().endsWith(".csv")).mapToLong(p->{ try{return Files.lines(p).count()-1;}catch(Exception e){return 0;}}).sum(); } catch(Exception e){}
                }
                if (src.equals("worldbank")) {
                    Path wbSilver = root.resolve("data/silver/worldbank/worldbank_observations/observations.csv");
                    if (Files.exists(wbSilver)) try { s = Files.lines(wbSilver).count()-1; } catch(Exception e){}
                }
                sb.append(src).append(",").append(b).append(",").append(s).append("\n");
            }
            Files.writeString(outAll, sb.toString());
            System.out.println("Gold all_sources_summary -> "+outAll);
            generated++;
        }
        System.out.println("Gold aggregation done. Generated=" + generated + " Skipped=" + skipped + " Root=" + goldRoot);
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
