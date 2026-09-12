package com.hedgefund.yahoo.ingest;

import com.hedgefund.config.ConfigValidator;
import com.hedgefund.config.HedgeConfig;
import com.hedgefund.datalake.Datalake;
import com.hedgefund.observability.health.HealthChecker;
import com.hedgefund.yahoo.config.YahooConfig;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        String configPath = "config/sources.yaml";
        String cfgPath = "config/yahoo/yahoo.yaml";
        boolean dryRun = false;
        boolean showHealth = false;

        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) cfgPath = args[++i];
            if ("--sources-config".equals(args[i]) && i + 1 < args.length) configPath = args[++i];
            if ("--dry-run".equals(args[i])) dryRun = true;
            if ("--health".equals(args[i])) showHealth = true;
        }

        // Try centralized config first, fall back to per-source YAML
        Path root = Datalake.defaultLocal().getRoot();
        Path centralizedPath = root.getParent().resolve(configPath);

        if (showHealth) {
            HealthChecker checker = new HealthChecker(root);
            System.out.println(checker.checkAsJson());
            return;
        }

        YahooConfig cfg;
        if (Files.exists(centralizedPath)) {
            HedgeConfig hedgeConfig = HedgeConfig.load(centralizedPath);
            ConfigValidator.validateAndFailFast(hedgeConfig);
            HedgeConfig.SourceEntry source = hedgeConfig.getSource("yahoo");
            if (source != null) {
                cfg = new YahooConfig(
                    source.baseUrl(),
                    source.symbols(),
                    "1d",
                    "1mo",
                    com.hedgefund.ingest.config.IngestConfig.defaults("yahoo")
                );
            } else {
                cfg = YahooConfig.defaults();
            }
        } else {
            Path cfgFile = Path.of(cfgPath);
            if (!Files.exists(cfgFile)) {
                Path alt = root.getParent().resolve(cfgPath);
                if (Files.exists(alt)) cfgFile = alt;
            }
            cfg = Files.exists(cfgFile) ? YahooConfig.fromYaml(cfgFile) : YahooConfig.defaults();
        }

        if (dryRun) {
            System.out.println("DryRun symbols=" + cfg.symbols());
            return;
        }

        new YahooIngestService(cfg, root).run();
        System.out.println("Yahoo ingest done. Bronze=" +
            root.resolve(cfg.ingestConfig().paths().bronze()) +
            " Silver=" + root.resolve(cfg.ingestConfig().paths().silver()));
    }
}
