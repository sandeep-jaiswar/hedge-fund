package com.hedgefund.tencent.ingest;
import com.hedgefund.config.ConfigValidator;
import com.hedgefund.config.HedgeConfig;
import com.hedgefund.datalake.Datalake;
import com.hedgefund.observability.health.HealthChecker;
import com.hedgefund.tencent.config.TencentConfig;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        String configPath = "config/sources.yaml";
        String cfgPath = "config/tencent/tencent.yaml";
        boolean dryRun = false;
        boolean showHealth = false;
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) cfgPath = args[++i];
            if ("--sources-config".equals(args[i]) && i + 1 < args.length) configPath = args[++i];
            if ("--dry-run".equals(args[i])) dryRun = true;
            if ("--health".equals(args[i])) showHealth = true;
        }
        Path root = Datalake.defaultLocal().getRoot();
        if (showHealth) { new HealthChecker(root); System.out.println(new HealthChecker(root).checkAsJson()); return; }
        Path centralizedPath = root.getParent().resolve(configPath);
        TencentConfig cfg;
        if (Files.exists(centralizedPath)) {
            HedgeConfig hedgeConfig = HedgeConfig.load(centralizedPath);
            ConfigValidator.validateAndFailFast(hedgeConfig);
            cfg = TencentConfig.defaults();
        } else {
            Path cfgFile = Path.of(cfgPath);
            if (!Files.exists(cfgFile)) { Path alt = root.getParent().resolve(cfgPath); if (Files.exists(alt)) cfgFile = alt; }
            cfg = Files.exists(cfgFile) ? TencentConfig.fromYaml(cfgFile) : TencentConfig.defaults();
        }
        if (dryRun) { System.out.println("DryRun symbols=" + cfg.symbols()); return; }
        new TencentIngestService(cfg, root).run();
        System.out.println("Tencent ingest done.");
    }
}
