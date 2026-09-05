package com.hedgefund.yahoo.ingest;
import com.hedgefund.yahoo.config.YahooConfig;
import com.hedgefund.datalake.Datalake;
import java.nio.file.*;
public class Main {
    public static void main(String[] args) throws Exception {
        String cfgPath="config/yahoo/yahoo.yaml";
        boolean dryRun=false;
        for(int i=0;i<args.length;i++){ if(args[i].equals("--config")&&i+1<args.length) cfgPath=args[++i]; if(args[i].equals("--dry-run")) dryRun=true; }
        Path cfgFile=Path.of(cfgPath);
        if(!Files.exists(cfgFile)){
            Path alt=Datalake.defaultLocal().getRoot().getParent().resolve(cfgPath);
            if(Files.exists(alt)) cfgFile=alt;
        }
        YahooConfig cfg=Files.exists(cfgFile)? YahooConfig.fromYaml(cfgFile): YahooConfig.defaults();
        if(dryRun){ System.out.println("DryRun symbols="+cfg.symbols()); return; }
        Path root=Datalake.defaultLocal().getRoot();
        new YahooIngestService(cfg, root).run();
        System.out.println("Yahoo ingest done. Bronze="+root.resolve(cfg.paths().bronze())+" Silver="+root.resolve(cfg.paths().silver()));
    }
}
