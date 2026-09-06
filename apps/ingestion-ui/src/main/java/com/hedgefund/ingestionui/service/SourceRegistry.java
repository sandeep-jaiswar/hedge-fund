package com.hedgefund.ingestionui.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Single source of truth for 22 ingest sources — replaces duplicated List.of(Map) in controller + switch in IngestionJobService + build.gradle deps. */
@Component
public class SourceRegistry {
    public record SourceDef(String id, String name, String config, IngestRunner runner) {}
    @FunctionalInterface
    public interface IngestRunner { void run(Path configPath, Path datalakeRoot) throws Exception; }

    private final List<SourceDef> sources;

    public SourceRegistry() {
        sources = List.of(
            def("worldbank","World Bank WDI", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.worldbank.config.WorldBankConfig.fromYaml(cfg) : com.hedgefund.worldbank.config.WorldBankConfig.defaults();
                new com.hedgefund.worldbank.ingest.WorldBankIngestService(c, root).run();
            }),
            def("yahoo","Yahoo Finance", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.yahoo.config.YahooConfig.fromYaml(cfg) : com.hedgefund.yahoo.config.YahooConfig.defaults();
                new com.hedgefund.yahoo.ingest.YahooIngestService(c, root).run();
            }),
            def("cboe","CBOE (VIX)", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.cboe.config.CboeConfig.fromYaml(cfg) : com.hedgefund.cboe.config.CboeConfig.defaults();
                new com.hedgefund.cboe.ingest.CboeIngestService(c, root).run();
            }),
            def("binance","Binance", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.binance.config.BinanceConfig.fromYaml(cfg) : com.hedgefund.binance.config.BinanceConfig.defaults();
                new com.hedgefund.binance.ingest.BinanceIngestService(c, root).run();
            }),
            def("coinbase","Coinbase", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.coinbase.config.CoinbaseConfig.fromYaml(cfg) : com.hedgefund.coinbase.config.CoinbaseConfig.defaults();
                new com.hedgefund.coinbase.ingest.CoinbaseIngestService(c, root).run();
            }),
            def("defillama","DefiLlama", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.defillama.config.DefillamaConfig.fromYaml(cfg) : com.hedgefund.defillama.config.DefillamaConfig.defaults();
                new com.hedgefund.defillama.ingest.DefillamaIngestService(c, root).run();
            }),
            def("tencent","Tencent", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.tencent.config.TencentConfig.fromYaml(cfg) : com.hedgefund.tencent.config.TencentConfig.defaults();
                new com.hedgefund.tencent.ingest.TencentIngestService(c, root).run();
            }),
            def("sina","Sina", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.sina.config.SinaConfig.fromYaml(cfg) : com.hedgefund.sina.config.SinaConfig.defaults();
                new com.hedgefund.sina.ingest.SinaIngestService(c, root).run();
            }),
            def("eastmoney","EastMoney", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.eastmoney.config.EastmoneyConfig.fromYaml(cfg) : com.hedgefund.eastmoney.config.EastmoneyConfig.defaults();
                new com.hedgefund.eastmoney.ingest.EastmoneyIngestService(c, root).run();
            }),
            def("baostock","Baostock", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.baostock.config.BaostockConfig.fromYaml(cfg) : com.hedgefund.baostock.config.BaostockConfig.defaults();
                new com.hedgefund.baostock.ingest.BaostockIngestService(c, root).run();
            }),
            def("investing","Investing", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.investing.config.InvestingConfig.fromYaml(cfg) : com.hedgefund.investing.config.InvestingConfig.defaults();
                new com.hedgefund.investing.ingest.InvestingIngestService(c, root).run();
            }),
            def("fred","FRED", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.fred.config.FredConfig.fromYaml(cfg) : com.hedgefund.fred.config.FredConfig.defaults();
                new com.hedgefund.fred.ingest.FredIngestService(c, root).run();
            }),
            def("treasury","Treasury", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.treasury.config.TreasuryConfig.fromYaml(cfg) : com.hedgefund.treasury.config.TreasuryConfig.defaults();
                new com.hedgefund.treasury.ingest.TreasuryIngestService(c, root).run();
            }),
            def("sec","SEC EDGAR", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.sec.config.SecConfig.fromYaml(cfg) : com.hedgefund.sec.config.SecConfig.defaults();
                new com.hedgefund.sec.ingest.SecIngestService(c, root).run();
            }),
            def("imf","IMF", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.imf.config.ImfConfig.fromYaml(cfg) : com.hedgefund.imf.config.ImfConfig.defaults();
                new com.hedgefund.imf.ingest.ImfIngestService(c, root).run();
            }),
            def("oecd","OECD", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.oecd.config.OecdConfig.fromYaml(cfg) : com.hedgefund.oecd.config.OecdConfig.defaults();
                new com.hedgefund.oecd.ingest.OecdIngestService(c, root).run();
            }),
            def("calcfi","CalcFi", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.calcfi.config.CalcfiConfig.fromYaml(cfg) : com.hedgefund.calcfi.config.CalcfiConfig.defaults();
                new com.hedgefund.calcfi.ingest.CalcfiIngestService(c, root).run();
            }),
            def("fdic","FDIC", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.fdic.config.FdicConfig.fromYaml(cfg) : com.hedgefund.fdic.config.FdicConfig.defaults();
                new com.hedgefund.fdic.ingest.FdicIngestService(c, root).run();
            }),
            def("eia","EIA", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.eia.config.EiaConfig.fromYaml(cfg) : com.hedgefund.eia.config.EiaConfig.defaults();
                new com.hedgefund.eia.ingest.EiaIngestService(c, root).run();
            }),
            def("bls","BLS", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.bls.config.BlsConfig.fromYaml(cfg) : com.hedgefund.bls.config.BlsConfig.defaults();
                new com.hedgefund.bls.ingest.BlsIngestService(c, root).run();
            }),
            def("bea","BEA", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.bea.config.BeaConfig.fromYaml(cfg) : com.hedgefund.bea.config.BeaConfig.defaults();
                new com.hedgefund.bea.ingest.BeaIngestService(c, root).run();
            }),
            def("gmd","GMD", (cfg, root) -> {
                var c = Files.exists(cfg) ? com.hedgefund.gmd.config.GmdConfig.fromYaml(cfg) : com.hedgefund.gmd.config.GmdConfig.defaults();
                new com.hedgefund.gmd.ingest.GmdIngestService(c, root).run();
            })
        );
    }

    private static SourceDef def(String id, String name, IngestRunner runner) {
        return new SourceDef(id, name, "config/" + id + "/" + id + ".yaml", runner);
    }

    public List<SourceDef> all() { return sources; }

    public SourceDef get(String id) {
        return sources.stream().filter(s -> s.id().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown source: " + id));
    }

    public List<Map<String, Object>> asControllerMaps() {
        return sources.stream().map(s -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", s.id()); m.put("name", s.name()); m.put("config", s.config());
            return m;
        }).toList();
    }
}
