package com.hedgefund.ingestionui.service;

import com.hedgefund.datalake.Datalake;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IngestionJobService {
    private static final Logger log = LoggerFactory.getLogger(IngestionJobService.class);
    private final JobScheduler scheduler;
    private final StorageProvider storageProvider;

    public IngestionJobService(JobScheduler scheduler, StorageProvider storageProvider) {
        this.scheduler = scheduler;
        this.storageProvider = storageProvider;
    }

    public UUID enqueue(String source, String config) {
        String cfg = config != null ? config : "config/" + source + "/" + source + ".yaml";
        log.info("Enqueue ingest source={} config={}", source, cfg);
        return scheduler.enqueue(() -> runIngest(source, cfg)).asUUID();
    }

    public UUID schedule(String source, String config, java.time.Instant when) {
        String cfg = config != null ? config : "config/" + source + "/" + source + ".yaml";
        return scheduler.schedule(when, () -> runIngest(source, cfg)).asUUID();
    }

    @Job(name = "ingest-%0", retries = 1)
    public void runIngest(String source, String configPath) throws Exception {
        log.info("JobRunr starting ingest source={} config={}", source, configPath);
        Path cfg = Path.of(configPath);
        if (!cfg.isAbsolute()) {
            Path base = Path.of(System.getProperty("user.dir"));
            Path cand = base.resolve(cfg);
            if (Files.exists(cand)) cfg = cand;
            else if (Files.exists(base.resolve("config/" + source + "/" + source + ".yaml"))) cfg = base.resolve("config/" + source + "/" + source + ".yaml");
        }
        Path datalakeRoot = Datalake.defaultLocal().getRoot();

        switch (source.toLowerCase()) {
            case "worldbank" -> {
                var c = Files.exists(cfg) ? com.hedgefund.worldbank.config.WorldBankConfig.fromYaml(cfg) : com.hedgefund.worldbank.config.WorldBankConfig.defaults();
                new com.hedgefund.worldbank.ingest.WorldBankIngestService(c, datalakeRoot).run();
            }
            case "yahoo" -> {
                var c = Files.exists(cfg) ? com.hedgefund.yahoo.config.YahooConfig.fromYaml(cfg) : com.hedgefund.yahoo.config.YahooConfig.defaults();
                new com.hedgefund.yahoo.ingest.YahooIngestService(c, datalakeRoot).run();
            }
            case "cboe" -> {
                var c = Files.exists(cfg) ? com.hedgefund.cboe.config.CboeConfig.fromYaml(cfg) : com.hedgefund.cboe.config.CboeConfig.defaults();
                new com.hedgefund.cboe.ingest.CboeIngestService(c, datalakeRoot).run();
            }
            case "binance" -> {
                var c = Files.exists(cfg) ? com.hedgefund.binance.config.BinanceConfig.fromYaml(cfg) : com.hedgefund.binance.config.BinanceConfig.defaults();
                new com.hedgefund.binance.ingest.BinanceIngestService(c, datalakeRoot).run();
            }
            case "coinbase" -> {
                var c = Files.exists(cfg) ? com.hedgefund.coinbase.config.CoinbaseConfig.fromYaml(cfg) : com.hedgefund.coinbase.config.CoinbaseConfig.defaults();
                new com.hedgefund.coinbase.ingest.CoinbaseIngestService(c, datalakeRoot).run();
            }
            case "defillama" -> {
                var c = Files.exists(cfg) ? com.hedgefund.defillama.config.DefillamaConfig.fromYaml(cfg) : com.hedgefund.defillama.config.DefillamaConfig.defaults();
                new com.hedgefund.defillama.ingest.DefillamaIngestService(c, datalakeRoot).run();
            }
            case "tencent" -> {
                var c = Files.exists(cfg) ? com.hedgefund.tencent.config.TencentConfig.fromYaml(cfg) : com.hedgefund.tencent.config.TencentConfig.defaults();
                new com.hedgefund.tencent.ingest.TencentIngestService(c, datalakeRoot).run();
            }
            case "sina" -> {
                var c = Files.exists(cfg) ? com.hedgefund.sina.config.SinaConfig.fromYaml(cfg) : com.hedgefund.sina.config.SinaConfig.defaults();
                new com.hedgefund.sina.ingest.SinaIngestService(c, datalakeRoot).run();
            }
            case "eastmoney" -> {
                var c = Files.exists(cfg) ? com.hedgefund.eastmoney.config.EastmoneyConfig.fromYaml(cfg) : com.hedgefund.eastmoney.config.EastmoneyConfig.defaults();
                new com.hedgefund.eastmoney.ingest.EastmoneyIngestService(c, datalakeRoot).run();
            }
            case "baostock" -> {
                var c = Files.exists(cfg) ? com.hedgefund.baostock.config.BaostockConfig.fromYaml(cfg) : com.hedgefund.baostock.config.BaostockConfig.defaults();
                new com.hedgefund.baostock.ingest.BaostockIngestService(c, datalakeRoot).run();
            }
            case "investing" -> {
                var c = Files.exists(cfg) ? com.hedgefund.investing.config.InvestingConfig.fromYaml(cfg) : com.hedgefund.investing.config.InvestingConfig.defaults();
                new com.hedgefund.investing.ingest.InvestingIngestService(c, datalakeRoot).run();
            }
            case "fred" -> {
                var c = Files.exists(cfg) ? com.hedgefund.fred.config.FredConfig.fromYaml(cfg) : com.hedgefund.fred.config.FredConfig.defaults();
                new com.hedgefund.fred.ingest.FredIngestService(c, datalakeRoot).run();
            }
            case "treasury" -> {
                var c = Files.exists(cfg) ? com.hedgefund.treasury.config.TreasuryConfig.fromYaml(cfg) : com.hedgefund.treasury.config.TreasuryConfig.defaults();
                new com.hedgefund.treasury.ingest.TreasuryIngestService(c, datalakeRoot).run();
            }
            case "sec" -> {
                var c = Files.exists(cfg) ? com.hedgefund.sec.config.SecConfig.fromYaml(cfg) : com.hedgefund.sec.config.SecConfig.defaults();
                new com.hedgefund.sec.ingest.SecIngestService(c, datalakeRoot).run();
            }
            case "imf" -> {
                var c = Files.exists(cfg) ? com.hedgefund.imf.config.ImfConfig.fromYaml(cfg) : com.hedgefund.imf.config.ImfConfig.defaults();
                new com.hedgefund.imf.ingest.ImfIngestService(c, datalakeRoot).run();
            }
            case "oecd" -> {
                var c = Files.exists(cfg) ? com.hedgefund.oecd.config.OecdConfig.fromYaml(cfg) : com.hedgefund.oecd.config.OecdConfig.defaults();
                new com.hedgefund.oecd.ingest.OecdIngestService(c, datalakeRoot).run();
            }
            case "calcfi" -> {
                var c = Files.exists(cfg) ? com.hedgefund.calcfi.config.CalcfiConfig.fromYaml(cfg) : com.hedgefund.calcfi.config.CalcfiConfig.defaults();
                new com.hedgefund.calcfi.ingest.CalcfiIngestService(c, datalakeRoot).run();
            }
            case "fdic" -> {
                var c = Files.exists(cfg) ? com.hedgefund.fdic.config.FdicConfig.fromYaml(cfg) : com.hedgefund.fdic.config.FdicConfig.defaults();
                new com.hedgefund.fdic.ingest.FdicIngestService(c, datalakeRoot).run();
            }
            case "eia" -> {
                var c = Files.exists(cfg) ? com.hedgefund.eia.config.EiaConfig.fromYaml(cfg) : com.hedgefund.eia.config.EiaConfig.defaults();
                new com.hedgefund.eia.ingest.EiaIngestService(c, datalakeRoot).run();
            }
            case "bls" -> {
                var c = Files.exists(cfg) ? com.hedgefund.bls.config.BlsConfig.fromYaml(cfg) : com.hedgefund.bls.config.BlsConfig.defaults();
                new com.hedgefund.bls.ingest.BlsIngestService(c, datalakeRoot).run();
            }
            case "bea" -> {
                var c = Files.exists(cfg) ? com.hedgefund.bea.config.BeaConfig.fromYaml(cfg) : com.hedgefund.bea.config.BeaConfig.defaults();
                new com.hedgefund.bea.ingest.BeaIngestService(c, datalakeRoot).run();
            }
            case "gmd" -> {
                var c = Files.exists(cfg) ? com.hedgefund.gmd.config.GmdConfig.fromYaml(cfg) : com.hedgefund.gmd.config.GmdConfig.defaults();
                new com.hedgefund.gmd.ingest.GmdIngestService(c, datalakeRoot).run();
            }
            default -> throw new IllegalArgumentException("Unknown source: " + source);
        }
        log.info("Ingest done source={}", source);
    }

    public void delete(UUID jobId) {
        storageProvider.deletePermanently(jobId);
    }
}
