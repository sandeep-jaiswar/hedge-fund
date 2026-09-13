package com.hedgefund.gold;
import com.hedgefund.datalake.Datalake;
import java.nio.file.*;
import java.sql.*;
public class Main {
    public static void main(String[] args) throws Exception {
        Path root = Datalake.defaultLocal().getRoot();
        Path goldRoot = root.resolve("data/gold");
        Files.createDirectories(goldRoot);
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            // Yahoo OHLCV summary
            String yahooSilver = root.resolve("data/silver/yahoo/yahoo_ohlcv.csv").toString();
            if (Files.exists(Path.of(yahooSilver))) {
                Path out = goldRoot.resolve("yahoo_summary.csv");
                c.createStatement().execute("COPY (SELECT symbol, count(*) as bars, avg(close) as avg_close, max(high) as max_high, min(low) as min_low, sum(volume) as total_vol FROM read_csv('"+yahooSilver+"', header=true) GROUP BY symbol) TO '"+out.toString()+"' (HEADER, DELIMITER ',')");
                System.out.println("Gold yahoo_summary -> "+out);
            }
            // World Bank YoY
            String wbSilver = root.resolve("data/silver/worldbank/worldbank_observations/observations.csv").toString();
            if (Files.exists(Path.of(wbSilver))) {
                Path out = goldRoot.resolve("worldbank_yoy.csv");
                c.createStatement().execute("COPY (SELECT indicator_id, country_iso3, date, value, lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date) as prev, (value - lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date))/nullif(lag(value) OVER (PARTITION BY indicator_id, country_iso3 ORDER BY date),0) as yoy FROM read_csv('"+wbSilver+"', header=true) WHERE indicator_id='SP.POP.TOTL' LIMIT 1000) TO '"+out.toString()+"' (HEADER, DELIMITER ',')");
                System.out.println("Gold worldbank_yoy -> "+out);
            }
            // FRED real rates
            String fredSilver = root.resolve("data/silver/fred/fred.csv").toString();
            if (Files.exists(Path.of(fredSilver))) {
                Path out = goldRoot.resolve("fred_real_rates.csv");
                c.createStatement().execute("COPY (SELECT series_id, date, value FROM read_csv('" + fredSilver + "', header=true) WHERE series_id IN ('DGS10', 'T10Y2Y', 'DFF') ORDER BY series_id, date) TO '" + out.toString() + "' (HEADER, DELIMITER ',')");
                System.out.println("Gold fred_real_rates -> " + out);
            }
            // Treasury yield curve
            String treasurySilver = root.resolve("data/silver/treasury/treasury.csv").toString();
            if (Files.exists(Path.of(treasurySilver))) {
                Path out = goldRoot.resolve("treasury_yield_curve.csv");
                c.createStatement().execute("COPY (SELECT * FROM read_csv('" + treasurySilver + "', header=true) ORDER BY date) TO '" + out.toString() + "' (HEADER, DELIMITER ',')");
                System.out.println("Gold treasury_yield_curve -> " + out);
            }
            // EIA oil prices
            String eiaSilver = root.resolve("data/silver/eia/eia.csv").toString();
            if (Files.exists(Path.of(eiaSilver))) {
                Path out = goldRoot.resolve("eia_oil_prices.csv");
                c.createStatement().execute("COPY (SELECT * FROM read_csv('" + eiaSilver + "', header=true) ORDER BY date) TO '" + out.toString() + "' (HEADER, DELIMITER ',')");
                System.out.println("Gold eia_oil_prices -> " + out);
            }
            // SEC filings
            String secSilver = root.resolve("data/silver/sec/sec.csv").toString();
            if (Files.exists(Path.of(secSilver))) {
                Path out = goldRoot.resolve("sec_filings.csv");
                c.createStatement().execute("COPY (SELECT * FROM read_csv('" + secSilver + "', header=true)) TO '" + out.toString() + "' (HEADER, DELIMITER ',')");
                System.out.println("Gold sec_filings -> " + out);
            }
        }
        // Generic gold: count per source (file-based, no DuckDB needed)
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
