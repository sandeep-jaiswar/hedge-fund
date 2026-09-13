package com.hedgefund.runner;
import com.hedgefund.config.ConfigValidator;
import com.hedgefund.config.HedgeConfig;
import com.hedgefund.datalake.Datalake;
import com.hedgefund.observability.health.HealthChecker;
import com.hedgefund.yahoo.config.YahooConfig;
import com.hedgefund.yahoo.ingest.YahooIngestService;
import com.hedgefund.worldbank.config.WorldBankConfig;
import com.hedgefund.worldbank.ingest.WorldBankIngestService;
import com.hedgefund.baostock.config.BaostockConfig;
import com.hedgefund.baostock.ingest.BaostockIngestService;
import com.hedgefund.bea.config.BeaConfig;
import com.hedgefund.bea.ingest.BeaIngestService;
import com.hedgefund.binance.config.BinanceConfig;
import com.hedgefund.binance.ingest.BinanceIngestService;
import com.hedgefund.bls.config.BlsConfig;
import com.hedgefund.bls.ingest.BlsIngestService;
import com.hedgefund.calcfi.config.CalcfiConfig;
import com.hedgefund.calcfi.ingest.CalcfiIngestService;
import com.hedgefund.cboe.config.CboeConfig;
import com.hedgefund.cboe.ingest.CboeIngestService;
import com.hedgefund.coinbase.config.CoinbaseConfig;
import com.hedgefund.coinbase.ingest.CoinbaseIngestService;
import com.hedgefund.defillama.config.DefillamaConfig;
import com.hedgefund.defillama.ingest.DefillamaIngestService;
import com.hedgefund.eastmoney.config.EastmoneyConfig;
import com.hedgefund.eastmoney.ingest.EastmoneyIngestService;
import com.hedgefund.eia.config.EiaConfig;
import com.hedgefund.eia.ingest.EiaIngestService;
import com.hedgefund.fdic.config.FdicConfig;
import com.hedgefund.fdic.ingest.FdicIngestService;
import com.hedgefund.fred.config.FredConfig;
import com.hedgefund.fred.ingest.FredIngestService;
import com.hedgefund.gmd.config.GmdConfig;
import com.hedgefund.gmd.ingest.GmdIngestService;
import com.hedgefund.imf.config.ImfConfig;
import com.hedgefund.imf.ingest.ImfIngestService;
import com.hedgefund.investing.config.InvestingConfig;
import com.hedgefund.investing.ingest.InvestingIngestService;
import com.hedgefund.oecd.config.OecdConfig;
import com.hedgefund.oecd.ingest.OecdIngestService;
import com.hedgefund.sec.config.SecConfig;
import com.hedgefund.sec.ingest.SecIngestService;
import com.hedgefund.sina.config.SinaConfig;
import com.hedgefund.sina.ingest.SinaIngestService;
import com.hedgefund.tencent.config.TencentConfig;
import com.hedgefund.tencent.ingest.TencentIngestService;
import com.hedgefund.treasury.config.TreasuryConfig;
import com.hedgefund.treasury.ingest.TreasuryIngestService;
import java.nio.file.Files;
import java.nio.file.Path;

/** Single parameterized ingest runner: --source <id> (or HEDGE_SOURCE). Replaces 22 *-ingest mains. */
public class Main {

    interface Loader<T> { T load(Path p) throws java.io.IOException; }

    public static void main(String[] args) throws Exception {
        String source = System.getenv().getOrDefault("HEDGE_SOURCE", "");
        String configPath = "config/sources.yaml";
        String cfgPath = null;
        boolean dryRun = false;
        boolean showHealth = false;
        for (int i = 0; i < args.length; i++) {
            if ("--source".equals(args[i]) && i + 1 < args.length) source = args[++i];
            if ("--config".equals(args[i]) && i + 1 < args.length) cfgPath = args[++i];
            if ("--sources-config".equals(args[i]) && i + 1 < args.length) configPath = args[++i];
            if ("--dry-run".equals(args[i])) dryRun = true;
            if ("--health".equals(args[i])) showHealth = true;
        }
        if (source.isEmpty()) { System.err.println("Usage: ingest-runner --source <id> [--config <yaml>] [--dry-run] [--health]"); System.err.println("Sources: yahoo,worldbank," + String.join(",", SOURCES)); System.exit(2); }
        Path root = Datalake.defaultLocal().getRoot();
        if (showHealth) { System.out.println(new HealthChecker(root).checkAsJson()); return; }
        switch (source) {
            case "yahoo" -> {
                YahooConfig cfg = resolveYahoo(firstNonNull(cfgPath, "config/yahoo/yahoo.yaml"), configPath);
                if (dryRun) { System.out.println("DryRun symbols=" + cfg.symbols()); return; }
                new YahooIngestService(cfg, root).run();
                System.out.println(done(root, "yahoo", cfg.ingestConfig()));
            }
            case "worldbank" -> {
                WorldBankConfig cfg = resolve(firstNonNull(cfgPath, "config/worldbank/worldbank.yaml"), configPath, WorldBankConfig::defaults, WorldBankConfig::fromYaml);
                if (dryRun) { System.out.println(cfg); return; }
                new WorldBankIngestService(cfg, root).run();
                System.out.println(done(root, "worldbank", cfg.ingestConfig()));
                var catalogPath = root.resolve(cfg.ingestConfig().paths().catalog());
                if (Files.exists(catalogPath) && !Files.readString(catalogPath).contains("worldbank"))
                    org.slf4j.LoggerFactory.getLogger(Main.class).warn("Catalog missing worldbank tables; run provision or manually add. See datalake/catalog/glue.json");
            }
            case "baostock" -> {
                BaostockConfig cfg = resolve(firstNonNull(cfgPath, "config/baostock/baostock.yaml"), configPath, BaostockConfig::defaults, BaostockConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.symbols()); return; }
                new BaostockIngestService(cfg, root).run();
                System.out.println(done(root, "baostock", cfg.ingestConfig()));
            }
            case "bea" -> {
                BeaConfig cfg = resolve(firstNonNull(cfgPath, "config/bea/bea.yaml"), configPath, BeaConfig::defaults, BeaConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new BeaIngestService(cfg, root).run();
                System.out.println(done(root, "bea", cfg.ingestConfig()));
            }
            case "binance" -> {
                BinanceConfig cfg = resolve(firstNonNull(cfgPath, "config/binance/binance.yaml"), configPath, BinanceConfig::defaults, BinanceConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new BinanceIngestService(cfg, root).run();
                System.out.println(done(root, "binance", cfg.ingestConfig()));
            }
            case "bls" -> {
                BlsConfig cfg = resolve(firstNonNull(cfgPath, "config/bls/bls.yaml"), configPath, BlsConfig::defaults, BlsConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new BlsIngestService(cfg, root).run();
                System.out.println(done(root, "bls", cfg.ingestConfig()));
            }
            case "calcfi" -> {
                CalcfiConfig cfg = resolve(firstNonNull(cfgPath, "config/calcfi/calcfi.yaml"), configPath, CalcfiConfig::defaults, CalcfiConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new CalcfiIngestService(cfg, root).run();
                System.out.println(done(root, "calcfi", cfg.ingestConfig()));
            }
            case "cboe" -> {
                CboeConfig cfg = resolve(firstNonNull(cfgPath, "config/cboe/cboe.yaml"), configPath, CboeConfig::defaults, CboeConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.symbols()); return; }
                new CboeIngestService(cfg, root).run();
                System.out.println(done(root, "cboe", cfg.ingestConfig()));
            }
            case "coinbase" -> {
                CoinbaseConfig cfg = resolve(firstNonNull(cfgPath, "config/coinbase/coinbase.yaml"), configPath, CoinbaseConfig::defaults, CoinbaseConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.symbols()); return; }
                new CoinbaseIngestService(cfg, root).run();
                System.out.println(done(root, "coinbase", cfg.ingestConfig()));
            }
            case "defillama" -> {
                DefillamaConfig cfg = resolve(firstNonNull(cfgPath, "config/defillama/defillama.yaml"), configPath, DefillamaConfig::defaults, DefillamaConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.symbols()); return; }
                new DefillamaIngestService(cfg, root).run();
                System.out.println(done(root, "defillama", cfg.ingestConfig()));
            }
            case "eastmoney" -> {
                EastmoneyConfig cfg = resolve(firstNonNull(cfgPath, "config/eastmoney/eastmoney.yaml"), configPath, EastmoneyConfig::defaults, EastmoneyConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new EastmoneyIngestService(cfg, root).run();
                System.out.println(done(root, "eastmoney", cfg.ingestConfig()));
            }
            case "eia" -> {
                EiaConfig cfg = resolve(firstNonNull(cfgPath, "config/eia/eia.yaml"), configPath, EiaConfig::defaults, EiaConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new EiaIngestService(cfg, root).run();
                System.out.println(done(root, "eia", cfg.ingestConfig()));
            }
            case "fdic" -> {
                FdicConfig cfg = resolve(firstNonNull(cfgPath, "config/fdic/fdic.yaml"), configPath, FdicConfig::defaults, FdicConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new FdicIngestService(cfg, root).run();
                System.out.println(done(root, "fdic", cfg.ingestConfig()));
            }
            case "fred" -> {
                FredConfig cfg = resolve(firstNonNull(cfgPath, "config/fred/fred.yaml"), configPath, FredConfig::defaults, FredConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new FredIngestService(cfg, root).run();
                System.out.println(done(root, "fred", cfg.ingestConfig()));
            }
            case "gmd" -> {
                GmdConfig cfg = resolve(firstNonNull(cfgPath, "config/gmd/gmd.yaml"), configPath, GmdConfig::defaults, GmdConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new GmdIngestService(cfg, root).run();
                System.out.println(done(root, "gmd", cfg.ingestConfig()));
            }
            case "imf" -> {
                ImfConfig cfg = resolve(firstNonNull(cfgPath, "config/imf/imf.yaml"), configPath, ImfConfig::defaults, ImfConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new ImfIngestService(cfg, root).run();
                System.out.println(done(root, "imf", cfg.ingestConfig()));
            }
            case "investing" -> {
                InvestingConfig cfg = resolve(firstNonNull(cfgPath, "config/investing/investing.yaml"), configPath, InvestingConfig::defaults, InvestingConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new InvestingIngestService(cfg, root).run();
                System.out.println(done(root, "investing", cfg.ingestConfig()));
            }
            case "oecd" -> {
                OecdConfig cfg = resolve(firstNonNull(cfgPath, "config/oecd/oecd.yaml"), configPath, OecdConfig::defaults, OecdConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new OecdIngestService(cfg, root).run();
                System.out.println(done(root, "oecd", cfg.ingestConfig()));
            }
            case "sec" -> {
                SecConfig cfg = resolve(firstNonNull(cfgPath, "config/sec/sec.yaml"), configPath, SecConfig::defaults, SecConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new SecIngestService(cfg, root).run();
                System.out.println(done(root, "sec", cfg.ingestConfig()));
            }
            case "sina" -> {
                SinaConfig cfg = resolve(firstNonNull(cfgPath, "config/sina/sina.yaml"), configPath, SinaConfig::defaults, SinaConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new SinaIngestService(cfg, root).run();
                System.out.println(done(root, "sina", cfg.ingestConfig()));
            }
            case "tencent" -> {
                TencentConfig cfg = resolve(firstNonNull(cfgPath, "config/tencent/tencent.yaml"), configPath, TencentConfig::defaults, TencentConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.symbols()); return; }
                new TencentIngestService(cfg, root).run();
                System.out.println(done(root, "tencent", cfg.ingestConfig()));
            }
            case "treasury" -> {
                TreasuryConfig cfg = resolve(firstNonNull(cfgPath, "config/treasury/treasury.yaml"), configPath, TreasuryConfig::defaults, TreasuryConfig::fromYaml);
                if (dryRun) { System.out.println("DryRun keys=" + cfg.effectiveKeys()); return; }
                new TreasuryIngestService(cfg, root).run();
                System.out.println(done(root, "treasury", cfg.ingestConfig()));
            }
            default -> { System.err.println("Unknown source: " + source); System.exit(2); }
        }
    }

    private static final java.util.List<String> SOURCES = java.util.List.of(
        "yahoo","worldbank","baostock", "bea", "binance", "bls", "calcfi", "cboe", "coinbase", "defillama", "eastmoney", "eia", "fdic", "fred", "gmd", "imf", "investing", "oecd", "sec", "sina", "tencent", "treasury");

    private static String firstNonNull(String a, String b) { return a != null ? a : b; }

    static <T> T resolve(String cfgPath, String configPath, java.util.function.Supplier<T> defaults, Loader<T> loader) throws Exception {
        Path root = Datalake.defaultLocal().getRoot();
        Path centralizedPath = root.getParent().resolve(configPath);
        if (Files.exists(centralizedPath)) {
            HedgeConfig hedgeConfig = HedgeConfig.load(centralizedPath);
            ConfigValidator.validateAndFailFast(hedgeConfig);
        }
        Path cfgFile = Path.of(cfgPath);
        if (!Files.exists(cfgFile)) { Path alt = root.getParent().resolve(cfgPath); if (Files.exists(alt)) cfgFile = alt; }
        return Files.exists(cfgFile) ? loader.load(cfgFile) : defaults.get();
    }

    static YahooConfig resolveYahoo(String cfgPath, String configPath) throws Exception {
        Path root = Datalake.defaultLocal().getRoot();
        Path centralizedPath = root.getParent().resolve(configPath);
        if (Files.exists(centralizedPath)) {
            HedgeConfig hedgeConfig = HedgeConfig.load(centralizedPath);
            ConfigValidator.validateAndFailFast(hedgeConfig);
            HedgeConfig.SourceEntry src = hedgeConfig.getSource("yahoo");
            if (src != null) return new YahooConfig(src.baseUrl(), src.symbols(), "1d", "1mo", com.hedgefund.ingest.config.IngestConfig.defaults("yahoo"));
            return YahooConfig.defaults();
        }
        Path cfgFile = Path.of(cfgPath);
        if (!Files.exists(cfgFile)) { Path alt = root.getParent().resolve(cfgPath); if (Files.exists(alt)) cfgFile = alt; }
        return Files.exists(cfgFile) ? YahooConfig.fromYaml(cfgFile) : YahooConfig.defaults();
    }

    static String done(Path root, String source, com.hedgefund.ingest.config.IngestConfig ic) {
        return source + " ingest done. Bronze=" + root.resolve(ic.paths().bronze()) + " Silver=" + root.resolve(ic.paths().silver());
    }
}
