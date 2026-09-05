package com.hedgefund.gold;
import com.hedgefund.datalake.Datalake;
import java.nio.file.*;
import java.sql.*;
public class Main {
    public static void main(String[] args) throws Exception {
        Path root = Datalake.defaultLocal().getRoot();
        Path goldRoot = root.resolve("data/gold");
        Files.createDirectories(goldRoot);
        // Use direct DuckDB connections for gold (avoid QueryEngine DDL issue)
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            String yahooSilver = root.resolve("data/silver/yahoo/yahoo_ohlcv.csv").toString();
            if (Files.exists(Path.of(yahooSilver))) {
                Path out = goldRoot.resolve("yahoo_summary.csv");
                c.createStatement().execute("COPY (SELECT symbol, count(*) as bars, avg(close) as avg_close, max(high) as max_high, min(low) as min_low, sum(volume) as total_vol FROM read_csv('"+yahooSilver+"', header=true) GROUP BY symbol) TO '"+out.toString()+"' (HEADER, DELIMITER ',')");
                System.out.println("Gold yahoo_summary -> "+out);
            }
            String wbSilver = root.resolve("data/silver/worldbank/worldbank_observations/observations.csv").toString();
            if (Files.exists(Path.of(wbSilver))) {
                Path out = goldRoot.resolve("worldbank_yoy.csv");
                c.createStatement().execute("COPY (SELECT indicator_id, country_iso3, date, value, lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date) as prev, (value - lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date))/nullif(lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date),0) as yoy FROM read_csv('"+wbSilver+"', header=true) WHERE indicator_id='SP.POP.TOTL' LIMIT 1000) TO '"+out.toString()+"' (HEADER, DELIMITER ',')");
                System.out.println("Gold worldbank_yoy -> "+out);
            }
        }
        // Generic gold: count per source (file-based, no QueryEngine needed)
        {
            Path outAll = goldRoot.resolve("all_sources_summary.csv");
            StringBuilder sb = new StringBuilder("source,bronze_keys,silver_rows\n");
            for (String src : new String[]{"yahoo","binance","coinbase","defillama","worldbank","treasury","sec","fred","cboe","investing","tencent","sina","eastmoney","baostock","imf","oecd","calcfi","fdic","eia","bls","bea","gmd"}) {
                Path bronze = root.resolve("data/bronze/"+src);
                Path silver = root.resolve("data/silver/"+src);
                long b = Files.exists(bronze) ? Files.walk(bronze).filter(p->p.getFileName().toString().equals("data.raw")||p.getFileName().toString().equals("data.ndjson")).count() : 0;
                long s = 0;
                if (Files.exists(silver)) {
                    try { s = Files.walk(silver).filter(p->p.toString().endsWith(".csv")).mapToLong(p->{ try{return Files.lines(p).count()-1;}catch(Exception e){return 0;}}).sum(); } catch(Exception e){}
                }
                // worldbank silver is in subfolder worldbank_observations
                if (src.equals("worldbank")) {
                    Path wbSilver = root.resolve("data/silver/worldbank/worldbank_observations/observations.csv");
                    if (Files.exists(wbSilver)) s = Files.lines(wbSilver).count()-1;
                }
                sb.append(src).append(",").append(b).append(",").append(s).append("\n");
            }
            Files.writeString(outAll, sb.toString());
            System.out.println("Gold all_sources_summary -> "+outAll);
        }
        System.out.println("Gold aggregation done. Root="+goldRoot);
    }
}
