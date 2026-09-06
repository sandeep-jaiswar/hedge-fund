package com.hedgefund.coinbase.ingest;
import com.hedgefund.coinbase.client.CoinbaseClient;
import com.hedgefund.coinbase.config.CoinbaseConfig;
import com.hedgefund.coinbase.store.CoinbaseBronzeWriter;
import com.hedgefund.coinbase.store.CoinbaseSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class CoinbaseIngestService {
    private static final Logger log=LoggerFactory.getLogger(CoinbaseIngestService.class);
    private final CoinbaseConfig cfg;
    private final CoinbaseClient client;
    private final CoinbaseBronzeWriter bronze;
    private final CoinbaseSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public CoinbaseIngestService(CoinbaseConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new CoinbaseClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new CoinbaseBronzeWriter(bronzeRoot);
        this.silver=new CoinbaseSilverTransformer();
    }
    public void run() throws Exception {
        log.info("coinbase ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
        Semaphore sem=new Semaphore(cfg.concurrency());
        List<Future<?>> futures=new ArrayList<>();
        for(String key: cfg.effectiveKeys()){
            sem.acquire();
            futures.add(exec.submit(()->{
                try{
                    String url=buildUrl(key);
                    String raw=client.fetchRaw(url);
                    bronze.write(key, raw);
                    log.info("Done {} len={}", key, raw.length());
                }catch(Exception e){ log.error("Failed {}", key, e); throw new RuntimeException(e); }
                finally{ sem.release(); }
            }));
        }
        for(Future<?> f: futures) f.get(60, TimeUnit.SECONDS);
        }
        Path out=silver.transform(bronzeRoot, silverRoot, "coinbase.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return base+"/v2/prices/"+key+"/spot";
    }
}
