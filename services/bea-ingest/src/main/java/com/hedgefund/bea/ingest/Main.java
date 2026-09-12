package com.hedgefund.bea.ingest;

import com.hedgefund.config.ConfigValidator;
import com.hedgefund.config.HedgeConfig;
import com.hedgefund.datalake.Datalake;
import com.hedgefund.observability.health.HealthChecker;
import com.hedgefund.bea.config.BeaConfig;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        String configPath = "config/sources.yaml";
        String cfgPath = "config/bea/bea.yaml";
        boolean dryRun = false;
        boolean showHealth = false;

        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) cfgPath = args[++i];
            if ("--sources-config".equals(args[i]) && i + 1 < args.length) configPath = args[++i];
            if ("--dry-run".equals(args[i])) dryRun = true;
            if ("--health".equals(args[i])) showHealth = true;
        }

        Path root = Datalake.defaultLocal().getRoot();
        Path centralizedPath = root.getParent().resolve(configPath);

        if (showHealth) {
            HealthChecker checker = new HealthChecker(root);
            System.out.println(checker.checkAsJson());
            return;
        }

        BeaConfig cfg;
        if (Files.exists(centralizedPath)) {
            HedgeConfig hedgeConfig = HedgeConfig.load(centralizedPath);
            ConfigValidator.validateAndFailFast(hedgeConfig);
            cfg = BeaConfig.defaults();
        } else {
            Path cfgFile = Path.of(cfgPath);
            if (!Files.exists(cfgFile)) {
                Path alt = root.getParent().resolve(cfgPath);
                if (Files.exists(alt)) cfgFile = alt;
            }
            cfg = Files.exists(cfgFile) ? BeaConfig.fromYaml(cfgFile) : BeaConfig.defaults();
        }

        if (dryRun) {
            System.out.println("DryRun keys=" + cfg.effectiveKeys());
            return;
        }

        new BeaIngestService(cfg, root).run();
        System.out.println("bea ingest done. Bronze=" +
            root.resolve(cfg.ingestConfig().paths().bronze()) +
            " Silver=" + root.resolve(cfg.ingestConfig().paths().silver()));
    }
}
