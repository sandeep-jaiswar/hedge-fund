package com.hedgefund.worldbank.ingest;

import com.hedgefund.worldbank.client.WorldBankClient;
import com.hedgefund.worldbank.config.WorldBankConfig;
import com.hedgefund.worldbank.store.BronzeWriter;
import com.hedgefund.worldbank.store.SilverTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class WorldBankIngestService {
    private static final Logger log = LoggerFactory.getLogger(WorldBankIngestService.class);
    private final WorldBankConfig cfg;
    private final WorldBankClient client;
    private final BronzeWriter bronze;
    private final SilverTransformer silver;
    private final Path datalakeRoot;

    public WorldBankIngestService(WorldBankConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.datalakeRoot=datalakeRoot;
        this.client=new WorldBankClient(cfg);
        this.bronze=new BronzeWriter(datalakeRoot, cfg.paths().bronze());
        this.silver=new SilverTransformer(datalakeRoot, cfg.paths().bronze(), cfg.paths().silver());
    }

    public void run() throws Exception {
        List<String> indicators = cfg.indicators();
        if(cfg.fullCrawl() || indicators.isEmpty()){
            log.info("Full crawl enabled - listing all indicators");
            indicators = client.listAllIndicatorCodes();
            log.info("Full crawl {} indicators discovered", indicators.size());
        }
        List<List<String>> batches = chunk(indicators, cfg.maxIndicatorsPerRequest());
        List<String> countriesRaw = cfg.countries();
        final List<String> countries = countriesRaw.isEmpty() ? List.of("all") : countriesRaw;

        ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
        Semaphore conc = new Semaphore(cfg.concurrency());
        List<Future<?>> futures=new ArrayList<>();
        for(int idx=0; idx<batches.size(); idx++){
            final int batchIdx = idx;
            final List<String> b = batches.get(idx);
            conc.acquire();
            futures.add(exec.submit(()->{
                try{
                    log.info("Fetching batch {}/{} {} date {} countries {}", batchIdx+1, batches.size(), b, cfg.date(), countries);
                    var results = client.fetchAllPages(b, countries, cfg.date());
                    bronze.writeBatch(batchIdx, b, cfg.date(), results);
                    log.info("Batch {} done {} pages {} points", b, results.size(), results.stream().mapToInt(r->r.points().size()).sum());
                } catch(Exception e){ log.error("Batch {} failed", b, e); throw new RuntimeException(e); }
                finally { conc.release(); }
            }));
        }
        for(Future<?> f: futures) f.get();
        exec.shutdown();
        // silver
        silver.transform();
        // watermark
        Path wm = datalakeRoot.resolve(cfg.paths().bronze()).resolve("_watermark.json");
        java.nio.file.Files.writeString(wm, String.format("{\"lastRun\":\"%s\",\"indicators\":%d,\"countries\":\"%s\",\"date\":\"%s\"}", java.time.Instant.now().toString(), indicators.size(), String.join(";",countries), cfg.date()));
        log.info("Ingest complete, watermark {}", wm);
    }

    private <T> List<List<T>> chunk(List<T> list, int size){
        List<List<T>> out=new ArrayList<>();
        for(int i=0;i<list.size();i+=size) out.add(list.subList(i, Math.min(i+size, list.size())));
        return out;
    }
}
