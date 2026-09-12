package com.hedgefund.coinbase.ingest;
import com.hedgefund.coinbase.client.CoinbaseClient;
import com.hedgefund.coinbase.config.CoinbaseConfig;
import com.hedgefund.coinbase.store.CoinbaseBronzeWriter;
import com.hedgefund.coinbase.store.CoinbaseSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
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
        this.bronzeRoot=datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze=new CoinbaseBronzeWriter(bronzeRoot);
        this.silver=new CoinbaseSilverTransformer();
    }
    public void run() throws Exception {
        CorrelationId.withContext("coinbase");
        try {
            log.info("coinbase ingest start keys={} base={}", cfg.symbols(), cfg.baseUrl());
            Files.createDirectories(bronzeRoot);
            Files.createDirectories(silverRoot);
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem=new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures=new ArrayList<>();
            for(String key: cfg.symbols()){
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
            log.info("coinbase ingest done.");
        } finally {
            CorrelationId.clear();
        }
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return base+"/v2/prices/"+key+"/spot";
    }
}
