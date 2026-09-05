package com.hedgefund.eastmoney.ingest;
import com.hedgefund.eastmoney.config.EastmoneyConfig;
import com.hedgefund.datalake.Datalake;
import java.nio.file.*;
public class Main {
    public static void main(String[] args) throws Exception {
        String cfgPath="config/eastmoney/eastmoney.yaml";
        boolean dryRun=false;
        for(int i=0;i<args.length;i++){ if(args[i].equals("--config")&&i+1<args.length) cfgPath=args[++i]; if(args[i].equals("--dry-run")) dryRun=true; }
        Path cfgFile=Path.of(cfgPath);
        if(!Files.exists(cfgFile)){
            Path alt=Datalake.defaultLocal().getRoot().getParent().resolve(cfgPath);
            if(Files.exists(alt)) cfgFile=alt;
        }
        EastmoneyConfig cfg=Files.exists(cfgFile)? EastmoneyConfig.fromYaml(cfgFile): EastmoneyConfig.defaults();
        if(dryRun){ System.out.println("DryRun keys="+cfg.effectiveKeys()); return; }
        Path root=Datalake.defaultLocal().getRoot();
        new EastmoneyIngestService(cfg, root).run();
        System.out.println("eastmoney ingest done. Bronze="+root.resolve(cfg.paths().bronze())+" Silver="+root.resolve(cfg.paths().silver()));
    }
}
