-- Auto-generated from Liquibase 010-reset.xml — DuckDB views for DBeaver
-- Open in DBeaver SQL Editor (Path hedge-fund.duckdb) and run all

-- ═══════════════════════════════════════════════════════════════════
-- GOLD ANALYTICS VIEWS
-- ═══════════════════════════════════════════════════════════════════
CREATE OR REPLACE VIEW hedge_gold.vw_returns_daily AS SELECT i.symbol, i.asset_class_id, f.date, f.close, f.volume, (f.close / lag(f.close) OVER (PARTITION BY f.instrument_id ORDER BY f.date) - 1) AS ret_1d FROM dim.fact_market_ohlcv f JOIN dim.dim_instrument i USING(instrument_id);
CREATE OR REPLACE VIEW hedge_gold.vw_returns_cumulative AS SELECT symbol, date, close, ret_1d, exp(sum(ln(1 + ret_1d)) OVER (PARTITION BY symbol ORDER BY date)) AS cum_return FROM hedge_gold.vw_returns_daily WHERE ret_1d IS NOT NULL;
CREATE OR REPLACE VIEW hedge_gold.vw_volatility_30d AS SELECT symbol, date, ret_1d, stddev(ret_1d) OVER (PARTITION BY symbol ORDER BY date ROWS 30 PRECEDING) * sqrt(252) AS vol_30d_annualized FROM hedge_gold.vw_returns_daily WHERE ret_1d IS NOT NULL;
CREATE OR REPLACE VIEW hedge_gold.vw_macro_yoy AS SELECT m.indicator_id, m.country_iso3, m.date, m.value, m.source_id, lag(m.value) OVER (PARTITION BY m.indicator_id, m.country_iso3 ORDER BY m.date) AS prev_value, (m.value / nullif(lag(m.value) OVER (PARTITION BY m.indicator_id, m.country_iso3 ORDER BY m.date), 0) - 1) AS yoy_pct FROM dim.fact_macro_obs m;
CREATE OR REPLACE VIEW hedge_gold.vw_instrument_detail AS SELECT i.instrument_id, i.symbol, i.name, i.isin, ac.asset_class_code, ac.asset_class_name, c.currency_code, c.currency_name, s.sector_name, ind.industry_name, e.exchange_code, e.exchange_name, e.country_iso3, i.lot_size, i.valid_from, i.valid_to, i.is_current FROM dim.dim_instrument i JOIN dim.dim_asset_class ac ON ac.asset_class_id = i.asset_class_id JOIN dim.dim_currency c ON c.currency_id = i.currency_id JOIN dim.dim_sector s ON s.sector_id = i.sector_id JOIN dim.dim_industry ind ON ind.industry_id = s.industry_id JOIN dim.dim_exchange e ON e.exchange_id = i.exchange_id;
CREATE OR REPLACE VIEW hedge_gold.vw_source_coverage AS SELECT 'dim_instrument' AS table_name, count(*) AS row_count, count(DISTINCT _source) AS sources FROM dim.dim_instrument WHERE instrument_id > 0 UNION ALL SELECT 'fact_market_ohlcv', count(*), count(DISTINCT i._source) FROM dim.fact_market_ohlcv f JOIN dim.dim_instrument i USING(instrument_id) UNION ALL SELECT 'fact_macro_obs', count(*), count(DISTINCT source_id) FROM dim.fact_macro_obs UNION ALL SELECT 'fact_crypto_ohlcv', count(*), 0 FROM dim.fact_crypto_ohlcv UNION ALL SELECT 'fact_rates_daily', count(*), count(DISTINCT source_id) FROM dim.fact_rates_daily UNION ALL SELECT 'fact_filings', count(*), 0 FROM dim.fact_filings;

-- ═══════════════════════════════════════════════════════════════════
-- COMPAT VIEWS (backward-compatible table names)
-- ═══════════════════════════════════════════════════════════════════
CREATE OR REPLACE VIEW hedge_gold.yahoo_summary AS SELECT symbol, count(*) AS bars, avg(close) AS avg_close, max(high) AS max_high, min(low) AS min_low, sum(volume) AS total_vol FROM market.ohlcv GROUP BY symbol;
CREATE OR REPLACE VIEW hedge_gold.worldbank_yoy AS SELECT * FROM hedge_gold.vw_macro_yoy WHERE source_id='worldbank';
CREATE OR REPLACE VIEW hedge_gold.all_sources_summary AS SELECT * FROM hedge_gold.vw_source_coverage;
CREATE OR REPLACE VIEW hedge_silver.yahoo_ohlcv AS SELECT symbol, CAST(date AS DATE) AS trade_date, epoch, open, high, low, close, adj_close, volume FROM read_csv('datalake/data/silver/yahoo/yahoo_ohlcv.csv', header=true);
CREATE OR REPLACE VIEW hedge_silver.worldbank_observations AS SELECT * FROM dim.fact_macro_obs;
CREATE OR REPLACE VIEW hedge_silver.equity_master AS SELECT * FROM read_parquet('datalake/data/silver/master/equity/equity_master.parquet');

-- ═══════════════════════════════════════════════════════════════════
-- DOMAIN VIEWS
-- ═══════════════════════════════════════════════════════════════════
CREATE OR REPLACE VIEW market.ohlcv AS SELECT i.symbol, f.date, f.open, f.high, f.low, f.close, f.adj_close, f.volume, f.year FROM dim.fact_market_ohlcv f JOIN dim.dim_instrument i USING(instrument_id);
CREATE OR REPLACE VIEW market.crypto AS SELECT i.symbol, f.date, f.open, f.high, f.low, f.close, f.volume, f.trades, f.year FROM dim.fact_crypto_ohlcv f JOIN dim.dim_instrument i USING(instrument_id);
CREATE OR REPLACE VIEW macro.observation AS SELECT m.indicator_id, i.indicator_name, m.country_iso3, c.country_name, m.date, m.value, m.period_type, m.source_id FROM dim.fact_macro_obs m JOIN dim.dim_indicator i USING(indicator_id) JOIN dim.dim_country c USING(country_iso3);
CREATE OR REPLACE VIEW macro.rates AS SELECT r.indicator_id, r.date, r.value, r.source_id FROM dim.fact_rates_daily r;
CREATE OR REPLACE VIEW market.filings AS SELECT i.symbol, f.accession_number, f.filing_date, f.form_type, f.entity_name, f.url FROM dim.fact_filings f JOIN dim.dim_instrument i USING(instrument_id);

-- ═══════════════════════════════════════════════════════════════════
-- REF VIEWS (clean singular names)
-- ═══════════════════════════════════════════════════════════════════
CREATE OR REPLACE VIEW ref.currency AS SELECT currency_id AS id, currency_code AS code, currency_name AS name FROM dim.dim_currency;
CREATE OR REPLACE VIEW ref.asset_class AS SELECT asset_class_id AS id, asset_class_code AS code, asset_class_name AS name FROM dim.dim_asset_class;
CREATE OR REPLACE VIEW ref.industry AS SELECT industry_id AS id, industry_name AS name FROM dim.dim_industry;
CREATE OR REPLACE VIEW ref.sector AS SELECT sector_id AS id, sector_name AS name, industry_id FROM dim.dim_sector;
CREATE OR REPLACE VIEW ref.country AS SELECT country_iso3 AS code, country_name AS name, region FROM dim.dim_country;
CREATE OR REPLACE VIEW ref.indicator AS SELECT indicator_id AS id, indicator_name AS name, source_id, unit, frequency FROM dim.dim_indicator;
CREATE OR REPLACE VIEW ref.exchange AS SELECT exchange_id AS id, exchange_code AS code, exchange_name AS name, country_iso3, currency_id FROM dim.dim_exchange;

-- ═══════════════════════════════════════════════════════════════════
-- MASTER VIEWS
-- ═══════════════════════════════════════════════════════════════════
CREATE OR REPLACE VIEW master.instrument AS SELECT instrument_id AS id, symbol, name, asset_class_id, exchange_id, currency_id, sector_id, lot_size, isin, is_current FROM dim.dim_instrument;
CREATE OR REPLACE VIEW master.equity AS SELECT instrument_id AS id, outstanding_shares, market_cap, listing_date FROM dim.dim_equity;
CREATE OR REPLACE VIEW master.instrument_detail AS SELECT * FROM hedge_gold.vw_instrument_detail;
CREATE OR REPLACE VIEW master.equity_india AS SELECT i.symbol, i.name, i.isin, c.currency_code, e.exchange_code FROM dim.dim_instrument i JOIN dim.dim_currency c ON c.currency_id = i.currency_id JOIN dim.dim_exchange e ON e.exchange_id = i.exchange_id WHERE i.asset_class_id = 1 AND e.country_iso3 = 'IND' ORDER BY i.symbol;
