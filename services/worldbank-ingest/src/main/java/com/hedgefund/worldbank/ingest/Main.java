package com.hedgefund.worldbank.ingest;

import com.hedgefund.worldbank.config.WorldBankConfig;
import com.hedgefund.worldbank.ingest.WorldBankIngestService;
import com.hedgefund.datalake.Datalake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) throws Exception {
        String cfgPath = "config/worldbank/worldbank.yaml";
        boolean dryRun=false;
        for(int i=0;i<args.length;i++){
            if(args[i].equals("--config") && i+1<args.length) cfgPath=args[i+1];
            if(args[i].equals("--dry-run")) dryRun=true;
        }
        Path p = Path.of(cfgPath);
        if(!Files.exists(p)) p = Path.of("config/worldbank.yaml");
        if(!Files.exists(p)) p = Path.of("worldbank.yaml");
        WorldBankConfig cfg;
        if(Files.exists(p)){
            cfg = WorldBankConfig.fromYaml(p);
            log.info("Loaded config {}", p.toAbsolutePath());
        } else {
            cfg = WorldBankConfig.defaults();
            log.warn("Config not found {}, using defaults {}", cfgPath, cfg);
        }
        if(dryRun){ System.out.println(cfg); return; }

        Datalake lake = Datalake.defaultLocal();
        Path root = lake.getRoot();
        log.info("Datalake root {}", root);
        Files.createDirectories(root.resolve(cfg.paths().bronze()));
        Files.createDirectories(root.resolve(cfg.paths().silver()));
        var svc = new WorldBankIngestService(cfg, root);
        svc.run();
        System.out.println("World Bank ingest done. Bronze=" + root.resolve(cfg.paths().bronze()) + " Silver=" + root.resolve(cfg.paths().silver()));
        // ensure catalog entry exists
        var catalogPath = root.resolve(cfg.paths().catalog());
        if(Files.exists(catalogPath)){
            String catalog = Files.readString(catalogPath);
            if(!catalog.contains("worldbank")){
                log.warn("Catalog missing worldbank tables; run provision or manually add. See datalake/catalog/glue.json");
            }
        }
    }
}
