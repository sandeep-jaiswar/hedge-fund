package com.hedgefund.yahoo.ingest;
import com.hedgefund.yahoo.client.YahooClient;
import com.hedgefund.yahoo.config.YahooConfig;
import com.hedgefund.yahoo.model.Bar;
import com.hedgefund.yahoo.store.YahooBronzeWriter;
import com.hedgefund.yahoo.store.YahooSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class YahooIngestService {
    private static final Logger log=LoggerFactory.getLogger(YahooIngestService.class);
    private final YahooConfig cfg;
    private final YahooClient client;
    private final YahooBronzeWriter bronze;
    private final YahooSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public YahooIngestService(YahooConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new YahooClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new YahooBronzeWriter(bronzeRoot);
        this.silver=new YahooSilverTransformer();
    }
    public void run() throws Exception {
        log.info("Yahoo ingest start symbols={} interval={} range={}", cfg.symbols(), cfg.interval(), cfg.range());
        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);
        ExecutorService exec=Executors.newVirtualThreadPerTaskExecutor();
        Semaphore sem=new Semaphore(cfg.concurrency());
        List<Future<?>> futures=new ArrayList<>();
        ConcurrentHashMap<String, List<Bar>> all=new ConcurrentHashMap<>();
        for(String sym: cfg.symbols()){
            sem.acquire();
            futures.add(exec.submit(()->{
                try{
                    // fetch raw JSON via client (need raw for bronze)
                    List<Bar> bars=client.fetchChart(sym);
                    all.put(sym, bars);
                    // we need raw json again? fetch raw separately by calling http raw - simplified reuse bars json lines
                    // write bronze with bars
                    bronze.write(sym, bars, null);
                    log.info("Done {}", sym);
                }catch(Exception e){ log.error("Failed {}", sym, e); throw new RuntimeException(e); }
                finally{ sem.release(); }
            }));
        }
        for(Future<?> f: futures) f.get();
        exec.shutdown();
        Path out=silver.transform(bronzeRoot, silverRoot);
        long rows=Files.lines(out).count()-1;
        log.info("Silver wrote {} rows to {}", rows, out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"symbols\":"+cfg.symbols().size()+"}");
    }
}
