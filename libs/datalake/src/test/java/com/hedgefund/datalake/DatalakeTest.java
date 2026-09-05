package com.hedgefund.datalake;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

class DatalakeTest {

    @Test
    void localProvisionAndQuery() throws Exception {
        var lake = Datalake.defaultLocal();
        // catalog should be loadable (provision.py already ran)
        var cat = lake.loadCatalog();
        assertFalse(cat.databases().isEmpty(), "catalog databases empty");
        assertTrue(cat.databases().stream().anyMatch(d -> d.Name().equals("hedge_bronze")));

        // resolve S3 -> local
        Path p = lake.resolveS3("s3://hedge-bronze/market_ticks/");
        assertTrue(p.toString().contains("bronze/market_ticks"), "S3 resolve failed: " + p);

        // DuckDB query over CSV (Athena local) — use absolute path via lake.getRoot()
        String ticksCsv = lake.getRoot().resolve("data/bronze/market_ticks/market_ticks.csv").toString();
        String ohlcvCsv = lake.getRoot().resolve("data/silver/ohlcv/ohlcv.csv").toString();
        String posCsv = lake.getRoot().resolve("data/gold/positions/positions.csv").toString();
        try (var qe = new QueryEngine()) {
            List<Map<String, Object>> rows = qe.query(
                "SELECT count(*) as cnt FROM read_csv('" + ticksCsv + "', header=true)"
            );
            assertEquals(1, rows.size());
            long cnt = ((Number) rows.get(0).get("cnt")).longValue();
            assertEquals(200, cnt, "market_ticks should have 200 rows");

            // silver ohlcv
            var ohlcv = qe.query("SELECT symbol, count(*) as c FROM read_csv('" + ohlcvCsv + "', header=true) GROUP BY symbol ORDER BY symbol");
            assertEquals(5, ohlcv.size());

            // gold positions pnl sum
            var pnl = qe.query("SELECT sum(pnl) as total_pnl FROM read_csv('" + posCsv + "', header=true)");
            assertFalse(pnl.isEmpty());
            assertNotNull(pnl.get(0).get("total_pnl"));
        }
    }

    @Test
    void queryEngineHandlesJson() throws Exception {
        var lake = Datalake.defaultLocal();
        String ndjson = lake.getRoot().resolve("data/bronze/market_ticks/market_ticks.ndjson").toString();
        try (var qe = new QueryEngine()) {
            // NDJSON via read_json
            var rows = qe.query("SELECT count(*) as cnt FROM read_json('" + ndjson + "')");
            assertEquals(200, ((Number) rows.get(0).get("cnt")).longValue());
        }
    }
}
